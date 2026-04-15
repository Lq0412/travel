package com.lq.travel.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.lq.travel.service.AmapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AmapServiceImpl implements AmapService {

    @Value("${amap.key}")
    private String amapKey;

    @Value("${amap.timeout-ms:3000}")
    private int timeoutMs;

    private static final String GEO_URL = "https://restapi.amap.com/v3/config/district";
    private static final String WEATHER_URL = "https://restapi.amap.com/v3/weather/weatherInfo";
    private static final String GEOCODE_URL = "https://restapi.amap.com/v3/geocode/geo";

    @Override
    public String getWeatherInfo(String city) {
        String normalizedCity = normalizeCity(city);
        if (StrUtil.isBlank(normalizedCity) || StrUtil.isBlank(amapKey) || amapKey.startsWith("your_amap_web_service_key")) {
            return "";
        }

        try {
            // 1. 通过城市名查询 adcode
            String adcode = getAdcodeByCity(normalizedCity);
            if (StrUtil.isBlank(adcode)) {
                return "";
            }

            // 2. 根据 adcode 获取预报天气 (extensions=all 返回未来天气预报)
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("key", amapKey);
            paramMap.put("city", adcode);
            paramMap.put("extensions", "all");

            String response = HttpUtil.get(WEATHER_URL, paramMap, timeoutMs);
            JSONObject json = JSONUtil.parseObj(response);

            if ("1".equals(json.getStr("status"))) {
                JSONArray forecasts = json.getJSONArray("forecasts");
                if (forecasts != null && !forecasts.isEmpty()) {
                    JSONObject forecastData = forecasts.getJSONObject(0);
                    JSONArray casts = forecastData.getJSONArray("casts");
                    StringBuilder weatherStr = new StringBuilder();
                    weatherStr.append("目的地 ").append(normalizedCity).append(" 近期天气预报：\n");
                    for (int i = 0; i < casts.size(); i++) {
                        JSONObject cast = casts.getJSONObject(i);
                        weatherStr.append(cast.getStr("date")).append("，")
                                  .append("白天").append(cast.getStr("dayweather"))
                                  .append("/夜间").append(cast.getStr("nightweather")).append("，")
                                  .append("气温").append(cast.getStr("nighttemp")).append("~")
                                  .append(cast.getStr("daytemp")).append("℃\n");
                    }
                    return weatherStr.toString();
                }
            }
        } catch (Exception e) {
            log.warn("调用高德天气API获取 {} 天气失败: {}", normalizedCity, e.getMessage());
        }
        return "";
    }

    @Override
    public GeoPoint geocode(String city, String address) {
        String normalizedCity = normalizeCity(city);
        String normalizedAddress = StrUtil.trim(address);
        if (StrUtil.isBlank(normalizedAddress) || StrUtil.isBlank(amapKey) || amapKey.startsWith("your_amap_web_service_key")) {
            return null;
        }

        try {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("key", amapKey);
            paramMap.put("address", normalizedAddress);
            if (StrUtil.isNotBlank(normalizedCity)) {
                paramMap.put("city", normalizedCity);
            }

            String response = HttpUtil.get(GEOCODE_URL, paramMap, timeoutMs);
            JSONObject json = JSONUtil.parseObj(response);
            if (!"1".equals(json.getStr("status"))) {
                return null;
            }

            JSONArray geocodes = json.getJSONArray("geocodes");
            if (geocodes == null || geocodes.isEmpty()) {
                return null;
            }

            JSONObject firstGeocode = geocodes.getJSONObject(0);
            String location = firstGeocode.getStr("location");
            if (StrUtil.isBlank(location) || !location.contains(",")) {
                return null;
            }

            String[] parts = location.split(",");
            if (parts.length < 2) {
                return null;
            }

            return new GeoPoint(
                    Double.valueOf(parts[0]),
                    Double.valueOf(parts[1]),
                    StrUtil.blankToDefault(firstGeocode.getStr("formatted_address"), normalizedAddress)
            );
        } catch (Exception e) {
            log.warn("高德地理编码失败 city={}, address={}, error={}", normalizedCity, normalizedAddress, e.getMessage());
            return null;
        }
    }

    private String getAdcodeByCity(String city) {
        try {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("key", amapKey);
            paramMap.put("keywords", city);
            paramMap.put("subdistrict", "0");

            String response = HttpUtil.get(GEO_URL, paramMap, timeoutMs);
            JSONObject json = JSONUtil.parseObj(response);

            if ("1".equals(json.getStr("status"))) {
                JSONArray districts = json.getJSONArray("districts");
                if (districts != null && !districts.isEmpty()) {
                    return districts.getJSONObject(0).getStr("adcode");
                }
            }
        } catch (Exception e) {
            log.warn("获取城市 {} 的 adcode 失败: {}", city, e.getMessage());
        }
        return null;
    }

    private String normalizeCity(String city) {
        String normalized = StrUtil.trim(city);
        if (StrUtil.isBlank(normalized)) {
            return "";
        }

        String[] suffixes = {"怎么玩", "怎么游", "旅游", "旅行", "游玩", "出游", "出行", "玩"};
        for (String suffix : suffixes) {
            if (normalized.endsWith(suffix) && normalized.length() > suffix.length()) {
                normalized = normalized.substring(0, normalized.length() - suffix.length()).trim();
                break;
            }
        }
        return normalized;
    }
}
