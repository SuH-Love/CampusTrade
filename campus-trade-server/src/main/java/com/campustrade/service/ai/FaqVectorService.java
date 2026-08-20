package com.campustrade.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

@Slf4j
@Service
public class FaqVectorService {

    private static final double SIMILARITY_THRESHOLD = 0.15;
    private static final int TOP_K = 3;

    private final List<FaqItem> faqItems = new ArrayList<>();
    private final List<Map<String, Double>> faqVectors = new ArrayList<>();
    private final Map<String, Double> idfMap = new HashMap<>();

    static class FaqItem {
        String question;
        String answer;
        String category;

        FaqItem(String question, String answer, String category) {
            this.question = question;
            this.answer = answer;
            this.category = category;
        }
    }

    @PostConstruct
    public void init() {
        initFaqData();
        computeIdf();
        for (FaqItem item : faqItems) {
            faqVectors.add(computeTfIdfVector(item.question));
        }
        log.info("FaqVectorService initialized: {} FAQ items loaded", faqItems.size());
    }

    private void initFaqData() {
        faqItems.add(new FaqItem("如何发布商品", "在首页点击\"发布商品\"按钮，填写商品标题、描述、价格、分类、成色和图片即可发布。发布前请确保商品信息真实准确。", "交易"));
        faqItems.add(new FaqItem("怎么买东西", "浏览商品列表，点击感兴趣的商品进入详情页，点击\"立即购买\"下单。支付完成后等待卖家发货。", "交易"));
        faqItems.add(new FaqItem("如何支付订单", "下单后可通过支付宝完成支付。在订单详情页点击\"去支付\"按钮，系统会跳转到支付宝支付页面。", "支付"));
        faqItems.add(new FaqItem("订单状态有哪些", "订单状态包括：待支付、待发货、待收货、已完成、已取消。待支付超时未支付会自动取消。", "订单"));
        faqItems.add(new FaqItem("怎么确认收货", "在\"我的订单\"中找到待收货的订单，点击\"确认收货\"按钮。确认收货后交易完成，可以评价卖家。", "订单"));
        faqItems.add(new FaqItem("如何申请退款", "如果遇到问题，可以在订单详情页联系卖家协商。如无法协商解决，请联系平台客服处理。", "订单"));
        faqItems.add(new FaqItem("怎么评价卖家", "订单完成后，在订单详情页点击\"评价\"按钮，可以对卖家进行评分和文字评价。", "订单"));
        faqItems.add(new FaqItem("如何修改个人信息", "在\"我的\"页面点击\"个人资料\"，可以修改头像、昵称、手机号等个人信息。", "用户"));
        faqItems.add(new FaqItem("忘记密码怎么办", "在登录页面点击\"忘记密码\"，通过注册手机号验证后可以重置密码。", "用户"));
        faqItems.add(new FaqItem("如何注册账号", "在登录页面点击\"注册\"，填写用户名、密码和手机号即可注册校园贸易平台账号。", "用户"));
        faqItems.add(new FaqItem("怎么收藏商品", "在商品详情页点击爱心图标即可收藏商品。收藏的商品可以在\"我的收藏\"中查看。", "交易"));
        faqItems.add(new FaqItem("如何搜索商品", "在首页搜索框输入关键词，可以搜索商品标题和描述。也可以通过分类筛选浏览商品。", "交易"));
        faqItems.add(new FaqItem("商品分类有哪些", "平台商品分为数码电子、图书教材、生活用品、服装鞋帽、运动健身、其他等分类。", "交易"));
        faqItems.add(new FaqItem("如何联系卖家", "在商品详情页点击\"联系卖家\"按钮，可以通过站内聊天与卖家沟通。", "交易"));
        faqItems.add(new FaqItem("怎么下架商品", "在\"我的发布\"中找到要下架的商品，点击\"下架\"按钮即可。下架后商品不再显示在列表中。", "交易"));
        faqItems.add(new FaqItem("平台收手续费吗", "校园贸易平台为在校师生提供二手交易服务，不收取任何手续费。", "平台"));
        faqItems.add(new FaqItem("如何举报违规商品", "在商品详情页点击\"举报\"按钮，选择举报原因并填写说明。平台会尽快处理您的举报。", "平台"));
        faqItems.add(new FaqItem("支持哪些支付方式", "目前支持支付宝在线支付。支付安全有保障，请放心使用。", "支付"));
        faqItems.add(new FaqItem("商品图片上传要求", "商品图片支持JPG、PNG格式，单张大小不超过5MB。建议上传清晰真实的商品照片。", "交易"));
        faqItems.add(new FaqItem("如何查看浏览历史", "系统会自动记录您浏览过的商品，在\"我的\"页面可以查看浏览历史。", "用户"));
        faqItems.add(new FaqItem("卖家不发货怎么办", "如果卖家长时间不发货，可以在订单详情页取消订单，或通过聊天联系卖家催促发货。", "订单"));
        faqItems.add(new FaqItem("如何修改头像", "在\"个人资料\"页面点击头像上传新图片即可。支持JPG、PNG格式。", "用户"));
        faqItems.add(new FaqItem("商品成色怎么填", "商品成色分为：全新、几乎全新、轻微使用痕迹、有明显使用痕迹。请如实描述商品状态。", "交易"));
        faqItems.add(new FaqItem("如何查看交易记录", "在\"我的订单\"中可以查看所有买入订单。在\"我的发布\"中可以查看卖出记录。", "订单"));
        faqItems.add(new FaqItem("平台安全吗", "平台采用实名认证机制，所有交易记录可追溯。建议当面交易或使用平台支付，切勿私下转账。", "平台"));
    }

    private Set<String> extractBigrams(String text) {
        Set<String> bigrams = new HashSet<>();
        if (text == null || text.length() < 2) return bigrams;
        String cleaned = text.replaceAll("[\\s\\p{Punct}]+", "").toLowerCase();
        for (int i = 0; i < cleaned.length() - 1; i++) {
            bigrams.add(cleaned.substring(i, i + 2));
        }
        return bigrams;
    }

    private Map<String, Double> computeTfVector(String text) {
        Map<String, Double> tf = new HashMap<>();
        Set<String> bigrams = extractBigrams(text);
        for (String bigram : bigrams) {
            tf.merge(bigram, 1.0, Double::sum);
        }
        int total = bigrams.size();
        if (total > 0) {
            for (String key : tf.keySet()) {
                tf.put(key, tf.get(key) / total);
            }
        }
        return tf;
    }

    private void computeIdf() {
        Map<String, Integer> docFreq = new HashMap<>();
        int totalDocs = faqItems.size();
        for (FaqItem item : faqItems) {
            Set<String> bigrams = extractBigrams(item.question);
            for (String bigram : bigrams) {
                docFreq.merge(bigram, 1, Integer::sum);
            }
        }
        for (Map.Entry<String, Integer> entry : docFreq.entrySet()) {
            idfMap.put(entry.getKey(), Math.log((double) totalDocs / (entry.getValue() + 1)));
        }
    }

    private Map<String, Double> computeTfIdfVector(String text) {
        Map<String, Double> tf = computeTfVector(text);
        Map<String, Double> tfidf = new HashMap<>();
        for (Map.Entry<String, Double> entry : tf.entrySet()) {
            double idf = idfMap.getOrDefault(entry.getKey(), 0.0);
            tfidf.put(entry.getKey(), entry.getValue() * idf);
        }
        return tfidf;
    }

    private double cosineSimilarity(Map<String, Double> v1, Map<String, Double> v2) {
        if (v1.isEmpty() || v2.isEmpty()) return 0.0;
        double dotProduct = 0.0;
        for (String key : v1.keySet()) {
            if (v2.containsKey(key)) {
                dotProduct += v1.get(key) * v2.get(key);
            }
        }
        double norm1 = Math.sqrt(v1.values().stream().mapToDouble(x -> x * x).sum());
        double norm2 = Math.sqrt(v2.values().stream().mapToDouble(x -> x * x).sum());
        if (norm1 == 0 || norm2 == 0) return 0.0;
        return dotProduct / (norm1 * norm2);
    }

    public List<FaqItem> search(String query, int topK) {
        Map<String, Double> queryVector = computeTfIdfVector(query);
        List<Map.Entry<FaqItem, Double>> scored = new ArrayList<>();
        for (int i = 0; i < faqItems.size(); i++) {
            double score = cosineSimilarity(queryVector, faqVectors.get(i));
            scored.add(new AbstractMap.SimpleEntry<>(faqItems.get(i), score));
        }
        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<FaqItem> results = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            if (scored.get(i).getValue() >= SIMILARITY_THRESHOLD) {
                results.add(scored.get(i).getKey());
            }
        }
        return results;
    }

    public String buildContext(String query) {
        List<FaqItem> matches = search(query, TOP_K);
        if (matches.isEmpty()) {
            return "";
        }
        StringBuilder context = new StringBuilder("以下是与用户问题相关的常见问答参考信息：\n\n");
        for (int i = 0; i < matches.size(); i++) {
            FaqItem item = matches.get(i);
            context.append(String.format("参考%d：\n问题：%s\n答案：%s\n\n", i + 1, item.question, item.answer));
        }
        context.append("请基于以上参考信息回答用户的问题。如果参考信息足以回答，请直接给出答案；如果用户的问题超出参考信息范围，请根据校园交易平台常识回答，并提醒用户可以联系人工客服。\n");
        return context.toString();
    }

    public boolean hasRelevantFaq(String query) {
        return !search(query, 1).isEmpty();
    }
}