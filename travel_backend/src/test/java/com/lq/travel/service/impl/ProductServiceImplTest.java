package com.lq.travel.service.impl;

import com.lq.travel.model.dto.product.ProductSaveRequest;
import com.lq.travel.model.entity.Product;
import com.lq.travel.model.entity.User;
import com.lq.travel.service.AmapService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceImplTest {

    @Test
    void shouldFillAddressCoordinatesWhenGeocodeSucceeds() {
        ProductServiceImpl service = spy(new ProductServiceImpl());
        AmapService amapService = mock(AmapService.class);
        ReflectionTestUtils.setField(service, "amapService", amapService);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        doAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(101L);
            return true;
        }).when(service).saveOrUpdate(productCaptor.capture());

        when(amapService.geocode(eq("汕头"), eq("汕头市龙湖区长平路 95 号")))
                .thenReturn(new AmapService.GeoPoint(116.70801, 23.37124, "汕头市龙湖区长平路 95 号"));

        ProductSaveRequest request = new ProductSaveRequest();
        request.setName("汕头牛肉火锅");
        request.setCity("汕头");
        request.setAddress("汕头市龙湖区长平路 95 号");
        request.setDescription("测试");
        request.setCover("https://example.com/cover.jpg");

        User user = new User();
        user.setId(1L);

        Long productId = service.saveProduct(request, user);

        assertEquals(101L, productId);
        assertEquals("汕头市龙湖区长平路 95 号", productCaptor.getValue().getAddress());
        assertEquals(116.70801, productCaptor.getValue().getLongitude());
        assertEquals(23.37124, productCaptor.getValue().getLatitude());
        verify(amapService).geocode("汕头", "汕头市龙湖区长平路 95 号");
    }

    @Test
    void shouldKeepCoordinatesEmptyWhenGeocodeReturnsNull() {
        ProductServiceImpl service = spy(new ProductServiceImpl());
        AmapService amapService = mock(AmapService.class);
        ReflectionTestUtils.setField(service, "amapService", amapService);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        doReturn(true).when(service).saveOrUpdate(productCaptor.capture());
        when(amapService.geocode(eq("潮州"), eq("潮州市湘桥区牌坊街 1 号"))).thenReturn(null);

        ProductSaveRequest request = new ProductSaveRequest();
        request.setName("牌坊街腐乳饼");
        request.setCity("潮州");
        request.setAddress("潮州市湘桥区牌坊街 1 号");
        request.setDescription("测试");
        request.setCover("https://example.com/cover.jpg");

        User user = new User();
        user.setId(2L);

        service.saveProduct(request, user);

        assertEquals("潮州市湘桥区牌坊街 1 号", productCaptor.getValue().getAddress());
        assertNull(productCaptor.getValue().getLongitude());
        assertNull(productCaptor.getValue().getLatitude());
    }
}
