package com.campustrade.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class PlatformKnowledgeService {

    @Autowired
    private com.campustrade.mapper.AiKnowledgeMapper aiKnowledgeMapper;

    @Autowired
    private DeepSeekClient deepSeekClient;

    private static final String[][] PLATFORM_KNOWLEDGE_BLOCKS = {
        {"密码与账号", "密码 注册 登录 重置 忘记 账号 邮箱 验证码 锁定",
         "### 密码与账号\n" +
         "- 注册：需用户名+密码，可选绑定手机号和邮箱。密码要求8-50位，需包含大小写字母、数字、特殊字符中的三种。\n" +
         "- 重置密码：在登录页点击\"忘记密码\"，需输入用户名和注册时绑定的**邮箱**（非手机号），系统发送6位验证码到邮箱，验证后设置新密码。验证码5分钟有效。\n" +
         "- 登录安全：连续5次密码错误锁定账号30分钟。\n" +
         "- 如果用户说忘记密码，引导其通过邮箱验证码重置，不要提到手机号。\n" +
         "- 如果用户没有绑定邮箱，建议其联系管理员协助重置密码。\n\n"},
        {"商品发布与审核", "发布 审核 上架 下架 草稿 违禁 商品状态 怎么发布 如何发布 发布商品",
         "### 商品发布与审核\n" +
         "- **发布入口**：页面底部页脚有\"发布商品\"链接，点击进入发布页面(/goods/publish)\n" +
         "- **发布流程（三步表单）**：\n" +
         "  1. 第一步\"基本信息\"：填写标题(最多50字)、选择分类、选择成色、填写售价、原价(可选)、库存数量、描述(最多500字)\n" +
         "  2. 第二步\"图片上传\"：上传封面图(1张)和商品图片(最多5张)\n" +
         "  3. 第三步\"确认预览\"：预览商品信息，确认无误后点击提交\n" +
         "- **提交后**：商品以草稿状态保存，页面跳转到\"我的商品\"(/my-goods)，需在\"我的商品\"页面手动提交审核\n" +
         "- **审核流程**：提交审核 → AI自动审核内容合规性 → 通过(状态变为\"已审核\")/拒绝(状态变为\"审核拒绝\"并附原因)\n" +
         "- **上架**：审核通过后，在\"我的商品\"页面手动点击上架，商品才会在商品市场展示\n" +
         "- 商品状态：草稿(DRAFT)、待审核(PENDING)、已审核(APPROVED)、审核拒绝(REJECTED)、在售(ONLINE)、已下架(OFFLINE)、已售出(SOLD)\n" +
         "- 上架条件：只有\"已审核\"或\"已下架\"状态的商品才能上架\n\n"},
        {"订单交易", "订单 支付 发货 退款 评价 支付宝 配送 买家 危家",
         "### 订单交易\n" +
         "- 订单流程：待支付 → 已支付/待发货 → 配送中 → 待评价 → 已完成。可取消（待支付时）、退款（已支付后）。\n" +
         "- 支付方式：支付宝担保交易，买家付款后资金冻结在平台，确认收货后结算给卖家。\n" +
         "- 配送方式：快递配送或线下自提。\n" +
         "- 评价：确认收货后可对卖家评价（1-5星+文字）。\n\n"},
        {"其他功能", "收藏 购物车 关注 聊天 举报 通知 粉丝 小苏",
         "### 其他功能\n" +
         "- 收藏：可收藏感兴趣的商品。购物车：可加入购物车后批量下单。\n" +
         "- 关注：可关注其他用户，关注后其发布新商品会收到通知。\n" +
         "- 聊天：买卖双方可在线聊天沟通。举报：可举报违规商品或用户。\n" +
         "- 通知：订单状态变更、商品审核结果等会收到站内通知。\n\n"},
        {"前端页面导航", "在哪 怎么 如何 入口 页面 导航 去哪 哪里 哪个 个人中心 设置 profile",
         "### 前端页面结构与导航\n" +
         "#### 顶部导航栏（PC端）\n" +
         "- 从左到右：Logo+首页(/)、商品市场(/goods)、我的商品(/my-goods)、订单(/order)、收藏(/favorites)、关注(/following)\n" +
         "- 右侧图标：购物车(🛒)、聊天(💬)、通知(🔔)、用户头像下拉菜单（个人中心、收货地址、我的商品、设置）\n" +
         "- **发布商品入口**：页面底部页脚\"发布商品\"链接(/goods/publish)，或在用户下拉菜单中\n\n" +
         "#### 主要页面\n" +
         "- 首页(/)：推荐商品、分类入口、搜索栏\n" +
         "- 商品市场(/goods)：商品列表，支持搜索、分类筛选、排序\n" +
         "- 发布商品(/goods/publish)：填写商品信息，提交后进入AI审核\n" +
         "- 我的商品(/my-goods)：自己发布的商品列表，可编辑/上架/下架\n" +
         "- 订单(/order)：订单列表，支持按状态筛选，支持买家/卖家视角切换\n" +
         "- 个人中心(/profile)：标签页式布局\n" +
         "  - \"我的统计\"标签页：我的订单、出售商品、完成购物、收货地址、累计消费(¥)、累计收入(¥)\n" +
         "  - \"编辑资料\"标签页：修改昵称、手机号、邮箱\n" +
         "  - \"修改密码\"标签页 / \"实名认证\"标签页 / \"收款管理\"标签页\n" +
         "- 购物车(/cart) / 聊天(/chat) / 收货地址管理(/address)\n\n"},
        {"收货地址", "地址 收货",
         "### 收货地址入口\n" +
         "- 方式1：顶部导航栏右侧用户头像下拉菜单 → 点击\"收货地址\"\n" +
         "- 方式2：个人中心(/profile) → \"我的统计\"标签页 → 点击\"收货地址\"卡片\n" +
         "- 进入后点击\"新增地址\"按钮填写姓名、电话、省市区、详细地址\n\n"},
        {"资金流水", "流水 资金 消费 收入 钱包 资产 统计",
         "### 资金流水查看方式\n" +
         "- 平台没有独立的\"资产\"、\"钱包\"或\"流水\"页面，不要编造这些入口\n" +
         "- 查看总消费/总收入：个人中心(/profile) → \"我的统计\"标签页 → \"累计消费\"和\"累计收入\"卡片\n" +
         "- 查看具体订单资金流水：订单列表(/order) → 点击订单进入详情 → 页面下方有\"资金流水\"时间线\n" +
         "- 也可让AI直接查询：用户可说\"查一下订单XXX的资金流水\"，AI通过get_order_fund_logs工具查询\n\n"},
        {"实名认证", "实名 认证 身份 学生 学生证 认证",
         "### 实名认证\n" +
         "- 入口：个人中心(/profile) → \"实名认证\"标签页\n" +
         "- 流程：填写真实姓名 + 学号/身份证号 → 提交 → 系统验证 → 认证成功/失败\n" +
         "- 认证后可提升交易信任度，部分功能可能需要实名才能使用\n" +
         "- 认证信息保密存储，不会公开展示\n\n"},
        {"商品评价", "评价 评分 评论 打分 好评 差评 星级 rate",
         "### 商品评价\n" +
         "- 评价时机：确认收货后可对卖家评价\n" +
         "- 评价内容：1-5星评分 + 文字评论（可选）\n" +
         "- 评价入口：订单列表(/order) → 已完成订单 → \"评价\"按钮\n" +
         "- 查看评价：商品详情页可查看卖家评价列表和平均评分\n" +
         "- AI也可通过get_ratings工具查询卖家评价\n\n"},
        {"聊天功能", "聊天 消息 私信 在线沟通 聊天室 chat 联系卖家",
         "### 聊天功能\n" +
         "- 入口：顶部导航栏右侧聊天图标(💬)，或商品详情页\"联系卖家\"按钮\n" +
         "- 功能：买卖双方在线文字聊天，可发送图片\n" +
         "- 消息通知：收到新消息时导航栏聊天图标显示未读数\n" +
         "- 最近联系人：进入聊天页面可查看最近联系人列表\n" +
         "- AI可查询：未读消息数(get_unread_message_count)、最近联系人(get_recent_contacts)\n\n"},
        {"通知系统", "通知 消息提醒 站内信 系统通知 公告",
         "### 通知系统\n" +
         "- 入口：顶部导航栏右侧通知图标(🔔)\n" +
         "- 通知类型：订单状态变更、商品审核结果、系统公告、聊天新消息\n" +
         "- 未读通知数显示在图标上，点击进入通知列表\n" +
         "- 系统公告：管理员发布的重要通知，AI可通过get_announcements工具查询\n" +
         "- AI可查询通知列表(get_notifications工具)\n\n"}
    };

    private float[][] platformKnowledgeEmbeddings;
    private volatile boolean platformKnowledgeEmbeddingsReady = false;
    private List<String[]> activeKnowledgeBlocks = null;
    private volatile long knowledgeBlocksLoadTime = 0;

    public void invalidateCache() {
        activeKnowledgeBlocks = null;
        platformKnowledgeEmbeddingsReady = false;
    }

    private List<String[]> loadKnowledgeBlocks() {
        long now = System.currentTimeMillis();
        if (activeKnowledgeBlocks != null && now - knowledgeBlocksLoadTime < 300000) {
            return activeKnowledgeBlocks;
        }
        try {
            List<com.campustrade.entity.AiKnowledge> dbBlocks = aiKnowledgeMapper.selectAllEnabled();
            if (dbBlocks != null && !dbBlocks.isEmpty()) {
                List<String[]> blocks = new ArrayList<>();
                for (com.campustrade.entity.AiKnowledge k : dbBlocks) {
                    blocks.add(new String[]{k.getTitle(), k.getKeywords() != null ? k.getKeywords() : "", k.getContent()});
                }
                activeKnowledgeBlocks = blocks;
                knowledgeBlocksLoadTime = now;
                platformKnowledgeEmbeddingsReady = false;
                return blocks;
            }
        } catch (Exception e) {
            log.debug("Failed to load knowledge blocks from DB: {}", e.getMessage());
        }
        activeKnowledgeBlocks = java.util.Arrays.asList(PLATFORM_KNOWLEDGE_BLOCKS);
        knowledgeBlocksLoadTime = now;
        return activeKnowledgeBlocks;
    }

    private synchronized void initPlatformKnowledgeEmbeddings() {
        if (platformKnowledgeEmbeddingsReady) return;
        List<String[]> blocks = loadKnowledgeBlocks();
        try {
            List<String> texts = new ArrayList<>();
            for (String[] block : blocks) {
                texts.add(block[0] + " " + block[1]);
            }
            List<float[]> embs = deepSeekClient.embeddings(texts);
            if (embs != null && embs.size() == blocks.size()) {
                platformKnowledgeEmbeddings = embs.toArray(new float[0][]);
                platformKnowledgeEmbeddingsReady = true;
                log.info("Platform knowledge embeddings initialized: {} blocks", blocks.size());
            }
        } catch (Exception e) {
            log.warn("Failed to init platform knowledge embeddings: {}", e.getMessage());
        }
    }

    public String buildPlatformKnowledge(String userMessage) {
        String lower = userMessage != null ? userMessage.toLowerCase() : "";
        StringBuilder sb = new StringBuilder("\n\n## 平台知识（回答用户关于平台规则的问题时必须依据以下信息）\n");
        List<String[]> blocks = loadKnowledgeBlocks();

        List<int[]> embRanking = null;
        if (deepSeekClient.isEmbeddingAvailable()) {
            if (!platformKnowledgeEmbeddingsReady) {
                initPlatformKnowledgeEmbeddings();
            }
            if (platformKnowledgeEmbeddingsReady) {
                try {
                    float[] queryEmb = deepSeekClient.embedding(userMessage);
                    if (queryEmb != null) {
                        embRanking = new ArrayList<>();
                        for (int i = 0; i < blocks.size(); i++) {
                            double score = DeepSeekClient.cosineSim(queryEmb, platformKnowledgeEmbeddings[i]);
                            embRanking.add(new int[]{i, (int)(score * 10000)});
                        }
                        embRanking.sort((a, b) -> b[1] - a[1]);
                    }
                } catch (Exception e) {
                    log.debug("Semantic platform knowledge matching failed: {}", e.getMessage());
                }
            }
        }

        List<int[]> kwRanking = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            String[] kws = blocks.get(i)[1].split(" ");
            int matchCount = 0;
            for (String kw : kws) {
                if (lower.contains(kw)) matchCount++;
            }
            kwRanking.add(new int[]{i, matchCount});
        }
        kwRanking.sort((a, b) -> b[1] - a[1]);

        int rrfK = 60;
        Map<Integer, Double> rrfScores = new HashMap<>();
        if (embRanking != null) {
            for (int rank = 0; rank < embRanking.size(); rank++) {
                rrfScores.merge(embRanking.get(rank)[0], 1.0 / (rrfK + rank + 1), Double::sum);
            }
        }
        for (int rank = 0; rank < kwRanking.size(); rank++) {
            if (kwRanking.get(rank)[1] > 0) {
                rrfScores.merge(kwRanking.get(rank)[0], 1.0 / (rrfK + rank + 1), Double::sum);
            }
        }

        List<Map.Entry<Integer, Double>> fused = new ArrayList<>(rrfScores.entrySet());
        fused.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        int count = 0;
        for (Map.Entry<Integer, Double> entry : fused) {
            if (count >= 2) break;
            int blockIdx = entry.getKey();
            boolean embOk = false;
            if (embRanking != null) {
                for (int[] r : embRanking) {
                    if (r[0] == blockIdx && r[1] / 10000.0 >= 0.35) { embOk = true; break; }
                }
            }
            boolean kwOk = false;
            for (int[] r : kwRanking) {
                if (r[0] == blockIdx && r[1] > 0) { kwOk = true; break; }
            }
            if (!embOk && !kwOk) continue;
            sb.append(blocks.get(blockIdx)[2]);
            count++;
        }

        if (count == 0) {
            sb.append("### 平台概览\n")
              .append("- 校园二手交易平台，支持商品发布、订单交易、支付宝担保支付、收货地址管理、收藏/购物车/关注等功能。\n")
              .append("- 个人中心(/profile)包含统计、编辑资料、修改密码、实名认证、收款管理等标签页。\n")
              .append("- 具体功能位置请参考前端页面导航信息。\n\n");
        }

        sb.append("### 回答要求\n")
          .append("- 根据以上知识准确回答，不要编造不存在的功能。\n")
          .append("- 涉及具体数据时调用工具获取真实数据，不要凭空回答。\n")
          .append("- 如果用户发送了截图，先描述图片内容，再回答问题。");
        return sb.toString();
    }

    public String buildDateHint() {
        java.time.LocalDate today = java.time.LocalDate.now();
        String[] weekNames = {"一", "二", "三", "四", "五", "六", "日"};
        String weekName = weekNames[today.getDayOfWeek().getValue() - 1];
        return "\n\n当前日期：" + today + "（星期" + weekName + "）。" +
               "当用户提到'昨天'、'前天'、'近7天'等相对日期时，请根据当前日期计算具体日期，" +
                "并传给工具的startDate/endDate参数（格式yyyy-MM-dd）。";
    }
}