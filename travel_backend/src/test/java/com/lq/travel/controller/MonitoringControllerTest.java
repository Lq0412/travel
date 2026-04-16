package com.lq.travel.controller;

import com.lq.travel.common.ResponseDTO;
import com.lq.travel.service.AIUserContextService;
import com.lq.travel.service.QuotaService;
import com.lq.travel.service.impl.MilvusKnowledgeSyncService;
import com.lq.travel.service.impl.MilvusRagClient;
import com.lq.travel.util.AICacheHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonitoringControllerTest {

    private MonitoringController controller;
    private QuotaService quotaService;
    private AIUserContextService aiUserContextService;
    private MilvusKnowledgeSyncService milvusKnowledgeSyncService;
    private MilvusRagClient milvusRagClient;

    @BeforeEach
    void setUp() {
        controller = new MonitoringController();
        quotaService = mock(QuotaService.class);
        aiUserContextService = mock(AIUserContextService.class);
        milvusKnowledgeSyncService = mock(MilvusKnowledgeSyncService.class);
        milvusRagClient = mock(MilvusRagClient.class);

        ReflectionTestUtils.setField(controller, "cacheHandler", mock(AICacheHandler.class));
        ReflectionTestUtils.setField(controller, "quotaService", quotaService);
        ReflectionTestUtils.setField(controller, "aiUserContextService", aiUserContextService);
        ReflectionTestUtils.setField(controller, "milvusKnowledgeSyncService", milvusKnowledgeSyncService);
        ReflectionTestUtils.setField(controller, "milvusRagClient", milvusRagClient);
    }

    @Test
    void healthShouldExposeUpStatus() {
        ResponseEntity<ResponseDTO<Map<String, Object>>> response = controller.health();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("AI模块运行正常", response.getBody().getMessage());
        assertEquals("UP", response.getBody().getData().get("status"));
        assertNotNull(response.getBody().getData().get("timestamp"));
    }

    @Test
    void getQuotaShouldRequireLogin() {
        HttpServletRequest request = new MockHttpServletRequest();
        when(aiUserContextService.extractLoginUserId(request)).thenReturn(null);

        ResponseEntity<ResponseDTO<Map<String, Object>>> response = controller.getQuota(request);

        assertEquals("请先登录", response.getBody().getMessage());
    }

    @Test
    void getQuotaShouldReturnRemainingTokensForCurrentUser() {
        HttpServletRequest request = new MockHttpServletRequest();
        when(aiUserContextService.extractLoginUserId(request)).thenReturn(7L);
        when(quotaService.getRemainingQuota(7L)).thenReturn(888);

        ResponseEntity<ResponseDTO<Map<String, Object>>> response = controller.getQuota(request);

        assertEquals("获取配额成功", response.getBody().getMessage());
        assertEquals(7L, response.getBody().getData().get("userId"));
        assertEquals(888, response.getBody().getData().get("remainingQuota"));
    }

    @Test
    void resetUserQuotaShouldRejectNegativeQuota() {
        ResponseEntity<ResponseDTO<Void>> response = controller.resetUserQuota(1L, -1);

        assertEquals("配额值无效", response.getBody().getMessage());
    }

    @Test
    void syncMilvusKnowledgeShouldReturnServiceResult() {
        when(milvusKnowledgeSyncService.syncKnowledgeToMilvus(true))
                .thenReturn(Map.of("status", "SUCCESS", "upsertedDocs", 12));

        ResponseEntity<ResponseDTO<Map<String, Object>>> response = controller.syncMilvusKnowledge(true);

        assertEquals("Milvus知识库同步执行完成", response.getBody().getMessage());
        assertEquals("SUCCESS", response.getBody().getData().get("status"));
        verify(milvusKnowledgeSyncService).syncKnowledgeToMilvus(true);
    }

    @Test
    void queryMilvusCountShouldReturnClientResult() {
        when(milvusRagClient.queryEntityCount(5))
                .thenReturn(Map.of("queryCount", 5, "collection", "travel_knowledge"));

        ResponseEntity<ResponseDTO<Map<String, Object>>> response = controller.queryMilvusCount(5);

        assertEquals("Milvus可查询条数检查完成", response.getBody().getMessage());
        assertEquals(5, response.getBody().getData().get("queryCount"));
    }
}
