package com.campustrade;

import com.campustrade.common.Result;
import com.campustrade.common.ResultCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void testSuccess() {
        Result<String> result = Result.success("hello");
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals("hello", result.getData());
    }

    @Test
    void testSuccessNoData() {
        Result<Void> result = Result.success();
        assertEquals(200, result.getCode());
        assertNull(result.getData());
    }

    @Test
    void testError() {
        Result<Void> result = Result.error(400, "参数错误");
        assertEquals(400, result.getCode());
        assertEquals("参数错误", result.getMessage());
    }

    @Test
    void testErrorResultCode() {
        Result<Void> result = Result.error(ResultCode.UNAUTHORIZED);
        assertEquals(401, result.getCode());
        assertEquals("未登录", result.getMessage());
    }

    @Test
    void testAllResultCodes() {
        assertEquals(200, ResultCode.SUCCESS.getCode());
        assertEquals(400, ResultCode.BAD_REQUEST.getCode());
        assertEquals(401, ResultCode.UNAUTHORIZED.getCode());
        assertEquals(403, ResultCode.FORBIDDEN.getCode());
        assertEquals(404, ResultCode.NOT_FOUND.getCode());
        assertEquals(429, ResultCode.TOO_MANY_REQUESTS.getCode());
        assertEquals(500, ResultCode.INTERNAL_ERROR.getCode());
    }
}