package com.campustrade;

import com.campustrade.util.FileUploadUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileUploadUtilTest {

    @Test
    void testIsAllowedExtension() {
        assertTrue(FileUploadUtil.isAllowedExtension("test.jpg"));
        assertTrue(FileUploadUtil.isAllowedExtension("test.PNG"));
        assertTrue(FileUploadUtil.isAllowedExtension("photo.webp"));
        assertFalse(FileUploadUtil.isAllowedExtension("test.exe"));
        assertFalse(FileUploadUtil.isAllowedExtension("test.php"));
        assertFalse(FileUploadUtil.isAllowedExtension("test"));
        assertFalse(FileUploadUtil.isAllowedExtension(null));
    }

    @Test
    void testIsAllowedMimeType() {
        assertTrue(FileUploadUtil.isAllowedMimeType("image/jpeg"));
        assertTrue(FileUploadUtil.isAllowedMimeType("image/png"));
        assertFalse(FileUploadUtil.isAllowedMimeType("application/exe"));
        assertFalse(FileUploadUtil.isAllowedMimeType(null));
    }

    @Test
    void testIsFileSizeAllowed() {
        assertTrue(FileUploadUtil.isFileSizeAllowed(1024));
        assertTrue(FileUploadUtil.isFileSizeAllowed(10 * 1024 * 1024));
        assertFalse(FileUploadUtil.isFileSizeAllowed(0));
        assertFalse(FileUploadUtil.isFileSizeAllowed(-1));
        assertFalse(FileUploadUtil.isFileSizeAllowed(11 * 1024 * 1024));
    }

    @Test
    void testGenerateSafeFilename() {
        String safe = FileUploadUtil.generateSafeFilename("photo.jpg");
        assertNotNull(safe);
        assertTrue(safe.endsWith(".jpg"));
        assertNotEquals("photo.jpg", safe);

        String noExt = FileUploadUtil.generateSafeFilename("noext");
        assertNotNull(noExt);
    }

    @Test
    void testSanitizePath() {
        assertEquals("abc", FileUploadUtil.sanitizePath("abc"));
        assertEquals("abc", FileUploadUtil.sanitizePath("../abc"));
        assertEquals("abc", FileUploadUtil.sanitizePath("/abc"));
        assertNull(FileUploadUtil.sanitizePath(null));
    }
}