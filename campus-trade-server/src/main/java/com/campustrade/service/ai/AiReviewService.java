package com.campustrade.service.ai;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiReviewService {

    @Autowired
    private DeepSeekClient deepSeekClient;

    private static final String MODERATION_SYSTEM_PROMPT =
            "你是校园贸易平台的AI内容审核员。请审核用户发布的商品信息是否合规。\n" +
            "审核标准：\n" +
            "1. 标题和描述不得包含违禁品（武器、毒品、药品、烟草、酒精等）\n" +
            "2. 不得包含虚假、欺诈、误导性信息\n" +
            "3. 不得包含人身攻击、歧视性言论\n" +
            "4. 不得包含联系方式绕过平台交易（微信号、QQ号等）\n" +
            "5. 价格应合理，不得明显异常\n\n" +
            "请严格按以下格式回复（不要添加其他内容）：\n" +
            "APPROVE - 如果内容合规\n" +
            "REJECT: 原因 - 如果内容不合规，简述拒绝原因";

    private static final String TITLE_OPTIMIZATION_PROMPT =
            "你是校园贸易平台的AI助手。请为以下商品生成一个更吸引人的标题。\n" +
            "要求：\n" +
            "1. 保留核心信息（品牌、型号、成色等）\n" +
            "2. 简洁明了，不超过30字\n" +
            "3. 突出卖点\n" +
            "请直接返回优化后的标题，不要添加解释。";

    private static final List<String> BLOCKED_KEYWORDS = Arrays.asList(
            "枪", "弹药", "武器", "毒品", "大麻", "海洛因", "冰毒",
            "处方药", "烟草", "香烟", "电子烟", "酒精", "白酒",
            "赌博", "色情", "代考", "作弊", "假证", "身份证代办",
            "微信加我", "QQ加我", "转账", "私下交易", "绕过平台"
    );

    @Data
    public static class ReviewResult {
        private boolean approved;
        private String reason;
        private String suggestedTitle;
        private boolean aiReviewed;
    }

    public ReviewResult review(String title, String description, String price, String categoryName) {
        ReviewResult result = new ReviewResult();
        result.setAiReviewed(false);

        for (String keyword : BLOCKED_KEYWORDS) {
            String combined = (title + " " + description).toLowerCase();
            if (combined.contains(keyword.toLowerCase())) {
                result.setApproved(false);
                result.setReason("商品信息包含违禁关键词：" + keyword);
                result.setAiReviewed(true);
                log.warn("Goods blocked by keyword filter: keyword={}, title={}", keyword, title);
                return result;
            }
        }

        if (!deepSeekClient.isEnabled()) {
            result.setApproved(false);
            result.setReason("AI审核不可用，待人工审核");
            result.setAiReviewed(false);
            return result;
        }

        try {
            String userContent = "商品标题：" + title + "\n商品描述：" +
                    (description != null ? description : "无描述") + "\n价格：" + price + "元\n分类：" + categoryName;

            List<Map<String, Object>> messages = Arrays.asList(
                    Map.of("role", "system", "content", MODERATION_SYSTEM_PROMPT),
                    Map.of("role", "user", "content", userContent)
            );

            String aiResponse = deepSeekClient.chat(messages);
            result.setAiReviewed(true);

            if (aiResponse != null && aiResponse.toUpperCase().startsWith("APPROVE")) {
                result.setApproved(true);
                result.setReason("AI审核通过");
                result.setSuggestedTitle(optimizeTitle(title, description));
            } else if (aiResponse != null && aiResponse.toUpperCase().startsWith("REJECT")) {
                result.setApproved(false);
                int colonIdx = aiResponse.indexOf("：");
                if (colonIdx < 0) colonIdx = aiResponse.indexOf(":");
                if (colonIdx > 0) {
                    String reason = aiResponse.substring(colonIdx + 1).trim();
                    int dashIdx = reason.indexOf("-");
                    if (dashIdx >= 0) {
                        reason = reason.substring(dashIdx + 1).trim();
                    }
                    result.setReason(reason.isEmpty() ? "AI审核未通过" : reason);
                } else {
                    result.setReason("AI审核未通过");
                }
            } else {
                result.setApproved(false);
                result.setReason("AI审核结果不确定，待人工审核");
                result.setAiReviewed(false);
            }
        } catch (Exception e) {
            log.error("AI review failed for goods: title={}", title, e);
            result.setApproved(false);
            result.setReason("AI审核异常，待人工审核");
            result.setAiReviewed(false);
        }

        return result;
    }

    private String optimizeTitle(String title, String description) {
        if (!deepSeekClient.isEnabled()) {
            return null;
        }
        try {
            String userContent = "原标题：" + title + "\n描述：" + (description != null ? description : "");
            List<Map<String, Object>> messages = Arrays.asList(
                    Map.of("role", "system", "content", TITLE_OPTIMIZATION_PROMPT),
                    Map.of("role", "user", "content", userContent)
            );
            String optimized = deepSeekClient.chat(messages);
            if (optimized != null && optimized.length() > 0 && optimized.length() <= 30) {
                return optimized.trim();
            }
        } catch (Exception e) {
            log.warn("Title optimization failed: {}", e.getMessage());
        }
        return null;
    }
}