package com.campustrade.service.ai;

import com.campustrade.entity.AiSafetyRule;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiReviewService {

    @Autowired
    private DeepSeekClient deepSeekClient;
    @Autowired
    private AiConfigService configService;

    private static final String FALLBACK_MODERATION_PROMPT =
            "你是校园贸易平台的AI内容审核员。请审核用户发布的商品信息是否合规。\n" +
            "审核标准：\n1. 标题和描述不得包含违禁品\n2. 不得包含虚假、欺诈信息\n3. 不得包含人身攻击\n" +
            "4. 不得包含联系方式绕过平台交易\n5. 价格应合理\n\n" +
            "请严格按以下格式回复：\nAPPROVE - 如果内容合规\nREJECT: 原因 - 如果内容不合规";

    private static final String FALLBACK_TITLE_PROMPT =
            "你是校园贸易平台的AI助手。请为以下商品生成一个更吸引人的标题。\n" +
            "要求：1. 保留核心信息 2. 简洁明了不超过30字 3. 突出卖点\n请直接返回优化后的标题。";

    private static final List<String> FALLBACK_BLOCKED_KEYWORDS = Arrays.asList(
            "枪", "弹药", "武器", "毒品", "大麻", "海洛因", "冰毒",
            "处方药", "烟草", "香烟", "电子烟", "酒精", "白酒",
            "赌博", "色情", "代考", "作弊", "假证", "身份证代办",
            "微信加我", "QQ加我", "转账", "私下交易", "绕过平台"
    );

    private String getModerationPrompt() {
        String prompt = configService.getPromptTemplate("review.moderation");
        return prompt != null && !prompt.isEmpty() ? prompt : FALLBACK_MODERATION_PROMPT;
    }

    private String getTitleOptimizationPrompt() {
        String prompt = configService.getPromptTemplate("review.title_optimize");
        return prompt != null && !prompt.isEmpty() ? prompt : FALLBACK_TITLE_PROMPT;
    }

    private List<String> getBlockedKeywords() {
        try {
            List<AiSafetyRule> rules = configService.getSafetyRules("blocked_keyword");
            if (rules != null && !rules.isEmpty()) {
                List<String> keywords = new ArrayList<>();
                for (AiSafetyRule rule : rules) {
                    keywords.add(rule.getRulePattern());
                }
                return keywords;
            }
        } catch (Exception e) {
            log.debug("从DB读取blocked_keyword失败，使用fallback: {}", e.getMessage());
        }
        return FALLBACK_BLOCKED_KEYWORDS;
    }

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

        for (String keyword : getBlockedKeywords()) {
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
                    Map.of("role", "system", "content", getModerationPrompt()),
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
                    Map.of("role", "system", "content", getTitleOptimizationPrompt()),
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
