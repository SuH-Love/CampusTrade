package com.campustrade.util;

import java.util.Arrays;
import java.util.List;

public class SensitiveWordUtil {

    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            "赌博", "色情", "暴力", "毒品", "枪支", "诈骗", "洗钱",
            "fuck", "shit", "damn", "ass"
    );

    public static boolean containsSensitiveWord(String text) {
        if (text == null || text.isEmpty()) return false;
        String lowerText = text.toLowerCase();
        for (String word : SENSITIVE_WORDS) {
            if (lowerText.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static String filterSensitiveWord(String text) {
        if (text == null || text.isEmpty()) return text;
        String result = text;
        for (String word : SENSITIVE_WORDS) {
            result = result.replaceAll("(?i)" + word, "***");
        }
        return result;
    }
}