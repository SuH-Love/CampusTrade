package com.campustrade;

import com.campustrade.common.PageResult;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class PageResultTest {

    @Test
    void testPageResultCreation() {
        PageResult<String> result = new PageResult<>(Arrays.asList("a", "b"), 100L);
        assertEquals(2, result.getList().size());
        assertEquals(100L, result.getTotal());
    }

    @Test
    void testEmptyPageResult() {
        PageResult<String> result = new PageResult<>(Collections.emptyList(), 0L);
        assertTrue(result.getList().isEmpty());
        assertEquals(0L, result.getTotal());
    }
}