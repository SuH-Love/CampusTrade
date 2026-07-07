package com.campustrade.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FileUploadUtil {

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp"
    ));

    private static final Set<String> ALLOWED_MIME_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp"
    ));

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    public static boolean isAllowedExtension(String filename) {
        if (filename == null || !filename.contains(".")) return false;
        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(ext);
    }

    public static boolean isAllowedMimeType(String mimeType) {
        return mimeType != null && ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase());
    }

    public static boolean isFileSizeAllowed(long size) {
        return size > 0 && size <= MAX_FILE_SIZE;
    }

    public static String generateSafeFilename(String originalFilename) {
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        return UUID.randomUUID().toString().replace("-", "") + ext;
    }

    public static String sanitizePath(String path) {
        if (path == null) return null;
        return path.replaceAll("\\.\\.", "").replaceAll("/", "").replaceAll("\\\\", "");
    }
}