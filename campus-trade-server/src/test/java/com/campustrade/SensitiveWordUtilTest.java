package com.campustrade;

import com.campustrade.util.SensitiveWordUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SensitiveWordUtilTest {

    @Test
    void testContainsSensitiveWord() {
        assertTrue(SensitiveWordUtil.containsSensitiveWord("这是赌博内容"));
        assertTrue(SensitiveWordUtil.containsSensitiveWord("fuck you"));
        assertFalse(SensitiveWordUtil.containsSensitiveWord("正常内容"));
        assertFalse(SensitiveWordUtil.containsSensitiveWord(null));
        assertFalse(SensitiveWordUtil.containsSensitiveWord(""));
    }

    @Test
    void testFilterSensitiveWord() {
        assertEquals("这是***内容", SensitiveWordUtil.filterSensitiveWord("这是赌博内容"));
        assertEquals("*** you", SensitiveWordUtil.filterSensitiveWord("fuck you"));
        assertEquals("正常内容", SensitiveWordUtil.filterSensitiveWord("正常内容"));
        assertNull(SensitiveWordUtil.filterSensitiveWord(null));
    }
}