package com.campustrade;

import com.campustrade.util.PasswordUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void testStrongPassword() {
        assertTrue(PasswordUtil.isStrongPassword("Abc12345"));
        assertTrue(PasswordUtil.isStrongPassword("Test@123"));
        assertTrue(PasswordUtil.isStrongPassword("P@ssw0rd"));
    }

    @Test
    void testWeakPassword() {
        assertFalse(PasswordUtil.isStrongPassword("12345678"));
        assertFalse(PasswordUtil.isStrongPassword("abcdefgh"));
        assertFalse(PasswordUtil.isStrongPassword("Abc12"));
        assertFalse(PasswordUtil.isStrongPassword(null));
        assertFalse(PasswordUtil.isStrongPassword(""));
    }

    @Test
    void testPasswordWithSpecialChars() {
        assertTrue(PasswordUtil.isStrongPassword("Abc@1234"));
        assertTrue(PasswordUtil.isStrongPassword("Test!123"));
    }

    @Test
    void testGetPasswordRequirement() {
        String req = PasswordUtil.getPasswordRequirement();
        assertNotNull(req);
        assertTrue(req.contains("8"));
    }
}