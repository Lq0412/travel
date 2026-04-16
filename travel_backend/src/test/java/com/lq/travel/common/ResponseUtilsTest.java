package com.lq.travel.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ResponseUtilsTest {

    @Test
    void successWithMessageShouldPreserveCustomMessage() {
        ResponseDTO<Boolean> response = ResponseUtils.success("注册成功", true);

        assertEquals(0, response.getCode());
        assertEquals("注册成功", response.getMessage());
        assertEquals(true, response.getData());
        assertNotNull(response.getTimestamp());
    }
}
