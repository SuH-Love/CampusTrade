package com.campustrade;

import com.campustrade.common.ResultCode;
import com.campustrade.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void testBusinessExceptionWithCodeAndMessage() {
        BusinessException ex = new BusinessException(400, "参数错误");
        assertEquals(400, ex.getCode());
        assertEquals("参数错误", ex.getMessage());
    }

    @Test
    void testBusinessExceptionWithResultCode() {
        BusinessException ex = new BusinessException(ResultCode.NOT_FOUND);
        assertEquals(404, ex.getCode());
        assertEquals("资源不存在", ex.getMessage());
    }

    @Test
    void testBusinessExceptionWithMessageOnly() {
        BusinessException ex = new BusinessException("自定义错误");
        assertEquals(500, ex.getCode());
        assertEquals("自定义错误", ex.getMessage());
    }
}