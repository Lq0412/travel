package com.lq.travel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lq.travel.model.vo.ContentImageVO;
import com.lq.travel.service.ContentImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pexels 图片服务
 */
@Slf4j
@Service
public class ContentImageServiceImpl implements ContentImageService {

    private static final int DEFAULT_PER_PAGE = 6;
    private static final int MAX_PER_PAGE = 12;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${pexels.api-key:}")
    private String apiKey;

    @Value("${pexels.base-url:https://api.pexels.com/v1}")
    private String baseUrl;

    @Value("${pexels.cache-minutes:30}")
    private Integer cacheMinutes;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public ContentImageServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public List<ContentImageVO> searchTravelImages(String query, Integer perPage) {
        String normalizedQuery = StringUtils.hasText(query) ? query.trim() : "travel";
        int safePerPage = normalizePerPage(perPage);
        String cacheKey = normalizedQuery.toLowerCase() + ":" + safePerPage;

        List<ContentImageVO> cached = readCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        if (!StringUtils.hasText(apiKey)) {
            log.warn("Pexels API key is not configured, skip image search query={}", normalizedQuery);
            return Collections.emptyList();
        }

        try {
            String requestUrl = buildSearchUrl(normalizedQuery, safePerPage);
            HttpRequest request = HttpRequest.newBuilder(URI.create(requestUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", apiKey)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Pexels search failed status={} query={}", response.statusCode(), normalizedQuery);
                return Collections.emptyList();
            }

            List<ContentImageVO> images = parseImages(response.body());
            writeCache(cacheKey, images);
            return images;
        } catch (Exception e) {
            log.warn("Pexels search error query={}", normalizedQuery, e);
            return Collections.emptyList();
        }
    }

    private String buildSearchUrl(String query, int perPage) {
        return baseUrl + "/search?query="
                + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&per_page="
                + perPage
                + "&page=1&orientation=landscape";
    }

    private int normalizePerPage(Integer perPage) {
        if (perPage == null) {
            return DEFAULT_PER_PAGE;
        }
        return Math.max(1, Math.min(perPage, MAX_PER_PAGE));
    }

    private List<ContentImageVO> parseImages(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode photos = root.path("photos");
        if (!photos.isArray()) {
            return Collections.emptyList();
        }

        List<ContentImageVO> result = new ArrayList<>();
        for (JsonNode photo : photos) {
            JsonNode src = photo.path("src");

            ContentImageVO item = new ContentImageVO();
            if (photo.hasNonNull("id")) {
                item.setId(photo.get("id").asLong());
            }
            item.setAlt(photo.path("alt").asText(""));
            item.setPhotographer(photo.path("photographer").asText(""));
            item.setPhotographerUrl(photo.path("photographer_url").asText(""));
            item.setPexelsUrl(photo.path("url").asText(""));
            item.setWidth(photo.hasNonNull("width") ? photo.get("width").asInt() : null);
            item.setHeight(photo.hasNonNull("height") ? photo.get("height").asInt() : null);
            item.setMediumUrl(src.path("medium").asText(""));
            item.setLargeUrl(src.path("large").asText(""));
            item.setLarge2xUrl(src.path("large2x").asText(""));
            item.setLandscapeUrl(src.path("landscape").asText(""));
            result.add(item);
        }
        return result;
    }

    private List<ContentImageVO> readCache(String cacheKey) {
        CacheEntry entry = cache.get(cacheKey);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() > entry.expiresAt) {
            cache.remove(cacheKey);
            return null;
        }
        return entry.data;
    }

    private void writeCache(String cacheKey, List<ContentImageVO> data) {
        long ttlMillis = Math.max(1, cacheMinutes == null ? 30 : cacheMinutes) * 60_000L;
        cache.put(cacheKey, new CacheEntry(System.currentTimeMillis() + ttlMillis, data));
    }

    private record CacheEntry(long expiresAt, List<ContentImageVO> data) {
    }
}
