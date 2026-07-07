package com.campustrade;

import com.campustrade.util.SnowflakeIdUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SnowflakeIdUtilTest {

    @Test
    void testNextIdUnique() {
        SnowflakeIdUtil util = SnowflakeIdUtil.getInstance();
        long id1 = util.nextId();
        long id2 = util.nextId();
        assertNotEquals(id1, id2);
        assertTrue(id1 > 0);
        assertTrue(id2 > 0);
    }

    @Test
    void testNextIdStr() {
        SnowflakeIdUtil util = SnowflakeIdUtil.getInstance();
        String idStr = util.nextIdStr();
        assertNotNull(idStr);
        assertTrue(Long.parseLong(idStr) > 0);
    }

    @Test
    void testMultipleIdsIncreasing() {
        SnowflakeIdUtil util = SnowflakeIdUtil.getInstance();
        long prev = util.nextId();
        for (int i = 0; i < 100; i++) {
            long curr = util.nextId();
            assertTrue(curr > prev, "IDs should be monotonically increasing");
            prev = curr;
        }
    }

    @Test
    void testInvalidMachineId() {
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdUtil(-1));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdUtil(32));
    }

    @Test
    void testValidMachineId() {
        assertDoesNotThrow(() -> new SnowflakeIdUtil(0));
        assertDoesNotThrow(() -> new SnowflakeIdUtil(31));
    }
}