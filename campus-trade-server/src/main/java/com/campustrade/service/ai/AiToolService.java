package com.campustrade.service.ai;

import com.campustrade.entity.*;
import com.campustrade.mapper.*;
import com.campustrade.service.GoodsService;
import com.campustrade.service.OrderService;
import com.campustrade.service.UserService;
import com.campustrade.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
public class AiToolService {

    @Autowired private UserMapper userMapper;
    @Autowired private GoodsMapper goodsMapper;
    @Autowired private OrderMapper orderMapper;
    @Autowired private CartMapper cartMapper;
    @Autowired private DeliveryAddressMapper addressMapper;
    @Autowired private SellerRatingMapper ratingMapper;
    @Autowired private NotificationMapper notificationMapper;
    @Autowired private ChatMessageMapper chatMessageMapper;
    @Autowired private UserFollowMapper followMapper;
    @Autowired private GoodsCategoryMapper categoryMapper;
    @Autowired private GoodsFavoriteMapper favoriteMapper;
    @Autowired private FundLogMapper fundLogMapper;
    @Autowired private AnnouncementMapper announcementMapper;
    @Autowired private ReportMapper reportMapper;
    @Autowired private OrderService orderService;
    @Autowired private GoodsService goodsService;
    @Autowired private UserService userService;

    private static final Map<String, String> STATUS_MAP = new LinkedHashMap<>();
    private static final Map<String, String> GOODS_STATUS_MAP = new LinkedHashMap<>();
    static {
        STATUS_MAP.put("PENDING_PAY", "待支付");
        STATUS_MAP.put("PAID", "已支付");
        STATUS_MAP.put("SHIPPED", "已发货");
        STATUS_MAP.put("COMPLETED", "已完成");
        STATUS_MAP.put("CANCELLED", "已取消");
        STATUS_MAP.put("FINISHED", "已完成");
        GOODS_STATUS_MAP.put("ONLINE", "在售");
        GOODS_STATUS_MAP.put("OFFLINE", "下架");
        GOODS_STATUS_MAP.put("PENDING_AUDIT", "待审核");
        GOODS_STATUS_MAP.put("REJECTED", "审核拒绝");
        GOODS_STATUS_MAP.put("SOLD_OUT", "售罄");
    }

    private static Map<String, Object> tool(String name, String desc, Map<String, ?> props) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("type", "function");
        Map<String, Object> fn = new LinkedHashMap<>();
        fn.put("name", name);
        fn.put("description", desc);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", "object");
        params.put("properties", props != null ? props : new LinkedHashMap<>());
        fn.put("parameters", params);
        t.put("function", fn);
        return t;
    }

    private static Map<String, Object> prop(String type, String desc) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", type);
        p.put("description", desc);
        return p;
    }

    private static Map<String, Object> propEnum(String type, String desc, List<String> enums) {
        Map<String, Object> p = prop(type, desc);
        p.put("enum", enums);
        return p;
    }

    private static Map<String, Object> props(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> {
            String g = a.getAuthority();
            return "ROLE_ADMIN".equals(g) || "ROLE_SUPER_ADMIN".equals(g);
        });
    }

    private static volatile List<Map<String, Object>> CACHED_BASE_TOOLS = null;
    private static volatile List<Map<String, Object>> CACHED_ADMIN_TOOLS = null;

    public List<Map<String, Object>> getToolDefinitions() {
        if (CACHED_BASE_TOOLS == null) {
            synchronized (AiToolService.class) {
                if (CACHED_BASE_TOOLS == null) {
                    buildToolCache();
                }
            }
        }
        List<Map<String, Object>> tools = new ArrayList<>(CACHED_BASE_TOOLS);
        if (isAdmin()) {
            tools.addAll(CACHED_ADMIN_TOOLS);
        }
        return tools;
    }

    private static void buildToolCache() {
        List<Map<String, Object>> base = new ArrayList<>();

        base.add(tool("get_order_status",
            "查询当前用户的订单列表。可查买家订单（我买的）或卖家订单（我卖出的）。当用户询问'我的订单'、'购买记录'、'卖出记录'、'我卖出的商品'、'销售情况'、'物流状态'时调用。不传orderId时返回订单列表。",
            props(
                "orderId", prop("integer", "订单数字ID（可选）。仅当用户提到数字ID时传入。"),
                "role", propEnum("string", "查询角色：buyer=我买的订单，seller=我卖出的订单。默认buyer。", Arrays.asList("buyer", "seller")),
                "status", propEnum("string", "订单状态过滤（可选）", Arrays.asList("PENDING_PAY", "PAID", "SHIPPED", "COMPLETED", "CANCELLED")))));

        base.add(tool("get_order_by_no",
            "按订单号查询特定订单的详细信息。当用户提到具体订单号（如CT开头的长字符串）时调用。",
            props("orderNo", prop("string", "订单号字符串，通常以CT开头"))));

        base.add(tool("search_goods",
            "搜索平台上的商品。当用户想找商品、问有什么东西卖、找某类商品时调用。",
            props(
                "keyword", prop("string", "搜索关键词"),
                "limit", prop("integer", "返回数量（可选，默认10，最大20）"))));

        base.add(tool("get_user_profile",
            "查询当前用户的个人信息（昵称、手机、邮箱、实名状态、头像）。当用户问'我的信息'、'我的资料'、'我实名认证了吗'时调用。",
            null));

        base.add(tool("get_user_stats",
            "查询当前用户的统计信息（发布商品数、在售数、买家订单数、卖家订单数、总消费、总收入）。当用户问'我一共花了多少'、'我赚了多少'、'我的统计'时调用。",
            null));

        base.add(tool("get_my_goods",
            "查询我发布的商品列表（含状态：在售/下架/审核中）。当用户问'我发布的商品'、'我的商品'、'我有哪些在卖'时调用。",
            null));

        base.add(tool("get_goods_detail",
            "查询商品详情（价格、库存、描述、卖家、浏览数、收藏数）。当用户问'这个商品多少钱'、'商品详情'、'ID为xxx的商品'时调用。",
            props("goodsId", prop("integer", "商品ID"))));

        base.add(tool("get_favorites",
            "查询我的收藏列表。当用户问'我收藏了哪些'、'我的收藏夹'时调用。",
            null));

        base.add(tool("get_cart",
            "查询我的购物车（商品、数量、总价）。当用户问'购物车里有什么'、'购物车总价'时调用。",
            null));

        base.add(tool("get_addresses",
            "查询我的收货地址列表。当用户问'我的收货地址'、'我有几个地址'时调用。",
            null));

        base.add(tool("get_ratings",
            "查询某卖家的评价列表和平均评分。当用户问'这个卖家评分怎样'、'看看评价'、'我的评价'时调用。不传sellerId默认查自己的卖家评价。",
            props("sellerId", prop("integer", "卖家用户ID（可选），不传则查自己的卖家评价"))));

        base.add(tool("get_notifications",
            "查询我的通知列表和未读数。当用户问'我有什么通知'、'有未读消息吗'时调用。",
            null));

        base.add(tool("get_unread_message_count",
            "查询聊天未读消息总数。当用户问'我有多少未读消息'、'未读聊天'时调用。",
            null));

        base.add(tool("get_recent_contacts",
            "查询最近聊天联系人列表。当用户问'最近谁给我发消息'、'最近聊天'时调用。",
            null));

        base.add(tool("get_follow_list",
            "查询关注/粉丝列表。当用户问'我关注了谁'、'我的粉丝'、'我的关注'时调用。",
            props(
                "type", propEnum("string", "following=我关注的，followers=我的粉丝", Arrays.asList("following", "followers")),
                "userId", prop("integer", "目标用户ID（可选），不传则查自己的"))));

        base.add(tool("get_categories",
            "查询商品分类列表。当用户问'有哪些分类'、'分类列表'时调用。",
            null));

        base.add(tool("get_order_fund_logs",
            "查询订单资金流水。当用户问'资金流水'、'退款到账了吗'、'订单CTxxx的流水'时调用。",
            props("orderId", prop("integer", "订单ID"))));

        base.add(tool("get_announcements",
            "查询系统公告。当用户问'有什么公告'、'平台通知'、'系统公告'时调用。",
            null));

        base.add(tool("cancel_order",
            "取消订单（仅未支付订单可取消）。当用户说'取消订单'、'帮我取消CTxxx'时调用。",
            props(
                "orderId", prop("integer", "订单ID"),
                "reason", prop("string", "取消原因（可选）"))));

        base.add(tool("confirm_receipt",
            "确认收货。当用户说'确认收货'、'收到货了'时调用。",
            props("orderId", prop("integer", "订单ID"))));

        base.add(tool("ship_order",
            "卖家发货（填快递单号）。当用户说'发货'、'帮我发货'时调用。",
            props(
                "orderId", prop("integer", "订单ID"),
                "trackingNo", prop("string", "快递单号"))));

        base.add(tool("request_refund",
            "申请退款。当用户说'我要退款'、'申请退款'时调用。",
            props(
                "orderId", prop("integer", "订单ID"),
                "reason", prop("string", "退款原因"))));

        base.add(tool("rate_order",
            "评价订单（打分+评论）。当用户说'给好评'、'评价订单'、'打分'时调用。",
            props(
                "orderId", prop("integer", "订单ID"),
                "rating", prop("integer", "评分1-5星"),
                "comment", prop("string", "评价内容（可选）"))));

        base.add(tool("toggle_favorite",
            "收藏或取消收藏商品。当用户说'收藏这个商品'、'取消收藏'时调用。",
            props("goodsId", prop("integer", "商品ID"))));

        base.add(tool("add_to_cart",
            "加入购物车。当用户说'加入购物车'、'放进购物车'时调用。",
            props(
                "goodsId", prop("integer", "商品ID"),
                "quantity", prop("integer", "数量（可选，默认1）"))));

        base.add(tool("toggle_follow_user",
            "关注或取关用户。当用户说'关注这个卖家'、'取消关注'时调用。",
            props("userId", prop("integer", "目标用户ID"))));

        base.add(tool("online_offline_goods",
            "商品上架或下架。当用户说'下架我的商品'、'上架'时调用。",
            props(
                "goodsId", prop("integer", "商品ID"),
                "action", propEnum("string", "online=上架，offline=下架", Arrays.asList("online", "offline")))));

        base.add(tool("add_address",
            "新增收货地址。当用户说'添加收货地址'、'新增地址'时调用。",
            props(
                "receiverName", prop("string", "收货人姓名"),
                "receiverPhone", prop("string", "收货人手机号"),
                "province", prop("string", "省"),
                "city", prop("string", "市"),
                "district", prop("string", "区"),
                "detailAddress", prop("string", "详细地址"))));

        base.add(tool("submit_report",
            "提交举报。当用户说'举报'、'投诉'时调用。",
            props(
                "targetType", propEnum("string", "举报目标类型：goods=商品，user=用户", Arrays.asList("goods", "user")),
                "targetId", prop("integer", "目标ID（商品ID或用户ID）"),
                "reason", prop("string", "举报原因"),
                "description", prop("string", "详细描述（可选）"))));

        List<Map<String, Object>> admin = new ArrayList<>();
        admin.add(tool("admin_dashboard",
            "管理员仪表盘统计（用户数、商品数、订单数、今日数据等）。仅管理员可用。当用户问'平台数据概览'、'今天有多少新用户'时调用。",
            null));

        admin.add(tool("admin_list_users",
            "管理员查看用户列表。仅管理员可用。当用户问'查用户'、'用户列表'、'被封禁的用户'时调用。",
            props(
                "keyword", prop("string", "搜索关键词（用户名/昵称，可选）"),
                "status", prop("integer", "用户状态：1=正常，0=封禁（可选）"))));

        admin.add(tool("admin_ban_user",
            "管理员封禁或解封用户。仅管理员可用。当用户说'封禁用户'、'解封用户'时调用。",
            props(
                "userId", prop("integer", "用户ID"),
                "action", propEnum("string", "ban=封禁，unban=解封", Arrays.asList("ban", "unban")))));

        admin.add(tool("admin_audit_goods",
            "管理员审核商品（通过或拒绝）。仅管理员可用。当用户说'审核商品'、'通过审核'、'拒绝商品'时调用。",
            props(
                "goodsId", prop("integer", "商品ID"),
                "action", propEnum("string", "pass=通过，reject=拒绝", Arrays.asList("pass", "reject")),
                "reason", prop("string", "拒绝原因（action=reject时必填）"))));

        admin.add(tool("admin_list_reports",
            "管理员查看举报列表。仅管理员可用。当用户问'有哪些举报'、'待处理举报'时调用。",
            props(
                "status", propEnum("string", "举报状态过滤（可选）", Arrays.asList("PENDING", "RESOLVED", "DISMISSED")))));

        admin.add(tool("admin_handle_refund",
            "管理员处理退款（同意或拒绝）。仅管理员可用。当用户说'同意退款'、'拒绝退款'时调用。",
            props(
                "orderId", prop("integer", "订单ID"),
                "action", propEnum("string", "approve=同意退款，reject=拒绝退款", Arrays.asList("approve", "reject")),
                "reason", prop("string", "拒绝原因（action=reject时可选）"))));

        CACHED_BASE_TOOLS = java.util.Collections.unmodifiableList(base);
        CACHED_ADMIN_TOOLS = java.util.Collections.unmodifiableList(admin);
    }

    public String executeTool(String toolName, Map<String, Object> arguments) {
        try {
            switch (toolName) {
                case "get_order_status": return executeGetOrderStatus(arguments);
                case "get_order_by_no": return executeGetOrderByNo(arguments);
                case "search_goods": return executeSearchGoods(arguments);
                case "get_user_profile": return executeGetUserProfile();
                case "get_user_stats": return executeGetUserStats();
                case "get_my_goods": return executeGetMyGoods();
                case "get_goods_detail": return executeGetGoodsDetail(arguments);
                case "get_favorites": return executeGetFavorites();
                case "get_cart": return executeGetCart();
                case "get_addresses": return executeGetAddresses();
                case "get_ratings": return executeGetRatings(arguments);
                case "get_notifications": return executeGetNotifications();
                case "get_unread_message_count": return executeGetUnreadMessageCount();
                case "get_recent_contacts": return executeGetRecentContacts();
                case "get_follow_list": return executeGetFollowList(arguments);
                case "get_categories": return executeGetCategories();
                case "get_order_fund_logs": return executeGetOrderFundLogs(arguments);
                case "get_announcements": return executeGetAnnouncements();
                case "cancel_order": return executeCancelOrder(arguments);
                case "confirm_receipt": return executeConfirmReceipt(arguments);
                case "ship_order": return executeShipOrder(arguments);
                case "request_refund": return executeRequestRefund(arguments);
                case "rate_order": return executeRateOrder(arguments);
                case "toggle_favorite": return executeToggleFavorite(arguments);
                case "add_to_cart": return executeAddToCart(arguments);
                case "toggle_follow_user": return executeToggleFollowUser(arguments);
                case "online_offline_goods": return executeOnlineOfflineGoods(arguments);
                case "add_address": return executeAddAddress(arguments);
                case "submit_report": return executeSubmitReport(arguments);
                case "admin_dashboard": return executeAdminDashboard();
                case "admin_list_users": return executeAdminListUsers(arguments);
                case "admin_ban_user": return executeAdminBanUser(arguments);
                case "admin_audit_goods": return executeAdminAuditGoods(arguments);
                case "admin_list_reports": return executeAdminListReports(arguments);
                case "admin_handle_refund": return executeAdminHandleRefund(arguments);
                default: return "未知工具: " + toolName;
            }
        } catch (Exception e) {
            log.error("Tool execution failed: {}", toolName, e);
            return "工具调用失败: " + e.getMessage();
        }
    }

    private String executeGetOrderStatus(Map<String, Object> arguments) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录后查看订单";

        Object orderIdObj = arguments.get("orderId");
        if (orderIdObj != null) {
            Long orderId = Long.valueOf(orderIdObj.toString());
            Order order = orderMapper.selectById(orderId);
            if (order == null || (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId))) {
                return "未找到订单 #" + orderId;
            }
            return formatOrder(order, userId);
        }

        String role = arguments.get("role") != null ? arguments.get("role").toString() : "buyer";
        String status = arguments.get("status") != null ? arguments.get("status").toString() : null;
        boolean isSeller = "seller".equalsIgnoreCase(role);

        List<Order> orders;
        Long totalCount;
        Long completedCount;

        if (isSeller) {
            orders = orderMapper.selectBySellerId(userId, status, 0, 10);
            totalCount = orderMapper.selectCountBySellerId(userId, null);
            Long c1 = orderMapper.selectCountBySellerId(userId, "COMPLETED");
            Long c2 = orderMapper.selectCountBySellerId(userId, "FINISHED");
            completedCount = (c1 != null ? c1 : 0) + (c2 != null ? c2 : 0);
        } else {
            orders = orderMapper.selectByBuyerId(userId, status, 0, 10);
            totalCount = orderMapper.selectCountByBuyerId(userId, null);
            Long c1 = orderMapper.selectCountByBuyerId(userId, "COMPLETED");
            Long c2 = orderMapper.selectCountByBuyerId(userId, "FINISHED");
            completedCount = (c1 != null ? c1 : 0) + (c2 != null ? c2 : 0);
        }

        String roleDesc = isSeller ? "卖家订单（您卖出的）" : "买家订单（您购买的）";
        if (orders == null || orders.isEmpty()) {
            return String.format("您还没有%s。总计: 0笔，已完成: 0笔", roleDesc);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("您的%s统计：总计%d笔，已完成%d笔\n", roleDesc, totalCount, completedCount));
        if (status != null) {
            sb.append(String.format("（已按状态'%s'过滤）\n", STATUS_MAP.getOrDefault(status, status)));
        }
        sb.append("订单列表：\n");
        for (Order order : orders) {
            sb.append(formatOrder(order, userId)).append("\n");
        }
        return sb.toString();
    }

    private String executeGetOrderByNo(Map<String, Object> arguments) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录后查看订单";

        Object orderNoObj = arguments.get("orderNo");
        if (orderNoObj == null || orderNoObj.toString().trim().isEmpty()) {
            return "请提供订单号";
        }

        String orderNo = orderNoObj.toString().trim();
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            return "未找到订单号为 " + orderNo + " 的订单";
        }
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            return "无权查看订单 " + orderNo;
        }
        return formatOrder(order, userId);
    }

    private String executeSearchGoods(Map<String, Object> arguments) {
        String keyword = arguments.get("keyword") != null ? arguments.get("keyword").toString() : "";
        Object limitObj = arguments.get("limit");
        int limit = 10;
        if (limitObj != null) {
            try { limit = Math.min(Math.max(Integer.parseInt(limitObj.toString()), 1), 20); } catch (Exception ignored) {}
        }
        List<Goods> goods = goodsMapper.selectList(null, keyword, null, null, "ONLINE", null, 0, limit);
        if (goods == null || goods.isEmpty()) return "没有找到相关商品";

        StringBuilder sb = new StringBuilder(String.format("找到以下商品（共%d件）：\n", goods.size()));
        for (Goods g : goods) {
            sb.append(formatGoods(g)).append("\n");
        }
        return sb.toString();
    }

    private String executeGetUserProfile() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";
        User user = userMapper.selectById(userId);
        if (user == null) return "用户不存在";
        return String.format("用户ID:%d | 用户名:%s | 昵称:%s | 手机:%s | 邮箱:%s | 实名认证:%s | 状态:%s",
                user.getId(), user.getUsername(), user.getNickname(),
                user.getPhone() != null ? user.getPhone() : "未绑定",
                user.getEmail() != null ? user.getEmail() : "未绑定",
                user.getRealVerified() != null && user.getRealVerified() == 1 ? "已认证" : "未认证",
                user.getStatus() != null && user.getStatus() == 1 ? "正常" : "封禁");
    }

    private String executeGetUserStats() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";

        Long goodsCount = goodsMapper.selectCountByStatusAndUserId(null, userId);
        Long onlineCount = goodsMapper.selectCountByStatusAndUserId("ONLINE", userId);
        Long buyerOrderCount = orderMapper.selectCountByBuyerId(userId, null);
        Long sellerOrderCount = orderMapper.selectCountBySellerId(userId, null);
        BigDecimal totalSpent = orderMapper.selectTotalSpentByBuyerId(userId);
        BigDecimal totalEarned = orderMapper.selectTotalEarnedBySellerId(userId);

        return String.format("您的统计信息：\n- 发布商品: %d件（在售%d件）\n- 买家订单: %d笔\n- 卖家订单: %d笔\n- 总消费: ¥%.2f\n- 总收入: ¥%.2f",
                goodsCount != null ? goodsCount : 0, onlineCount != null ? onlineCount : 0,
                buyerOrderCount != null ? buyerOrderCount : 0,
                sellerOrderCount != null ? sellerOrderCount : 0,
                totalSpent != null ? totalSpent.doubleValue() : 0,
                totalEarned != null ? totalEarned.doubleValue() : 0);
    }

    private String executeGetMyGoods() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";
        List<Goods> goods = goodsMapper.selectList(null, null, null, null, null, userId, 0, 10);
        if (goods == null || goods.isEmpty()) return "您还没有发布任何商品";
        StringBuilder sb = new StringBuilder("我的商品列表：\n");
        for (Goods g : goods) {
            sb.append(formatGoods(g)).append("\n");
        }
        return sb.toString();
    }

    private String executeGetGoodsDetail(Map<String, Object> arguments) {
        Long userId = SecurityUtil.getCurrentUserId();
        Object idObj = arguments.get("goodsId");
        if (idObj == null) return "请提供商品ID";
        Long goodsId = Long.valueOf(idObj.toString());
        Goods g = goodsMapper.selectById(goodsId);
        if (g == null) return "商品不存在";
        User seller = userMapper.selectById(g.getUserId());
        String sellerName = seller != null ? seller.getNickname() : "未知";
        return String.format("商品详情：\n- ID: %d\n- 标题: %s\n- 价格: ¥%.2f（原价¥%.2f）\n- 成色: %s\n- 库存: %d\n- 状态: %s\n- 浏览: %d | 收藏: %d\n- 卖家: %s（ID:%d）\n- 描述: %s",
                g.getId(), g.getTitle(),
                g.getPrice() != null ? g.getPrice().doubleValue() : 0,
                g.getOriginalPrice() != null ? g.getOriginalPrice().doubleValue() : 0,
                g.getCondition(), g.getStock(),
                GOODS_STATUS_MAP.getOrDefault(g.getStatus(), g.getStatus()),
                g.getViewCount() != null ? g.getViewCount() : 0,
                g.getFavoriteCount() != null ? g.getFavoriteCount() : 0,
                sellerName, g.getUserId(),
                g.getDescription() != null ? g.getDescription() : "无");
    }

    private String executeGetFavorites() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";
        List<Long> goodsIds = favoriteMapper.selectGoodsIdsByUserId(userId, 0, 10);
        if (goodsIds == null || goodsIds.isEmpty()) return "您还没有收藏任何商品";
        List<Goods> goods = goodsMapper.selectByIds(goodsIds);
        if (goods == null || goods.isEmpty()) return "您还没有收藏任何商品";
        StringBuilder sb = new StringBuilder(String.format("我的收藏（共%d件）：\n", favoriteMapper.selectCountByUserId(userId)));
        for (Goods g : goods) {
            sb.append(formatGoods(g)).append("\n");
        }
        return sb.toString();
    }

    private String executeGetCart() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";
        List<Cart> carts = cartMapper.selectByUserId(userId);
        if (carts == null || carts.isEmpty()) return "您的购物车是空的";
        List<Long> goodsIds = new ArrayList<>();
        for (Cart c : carts) goodsIds.add(c.getGoodsId());
        List<Goods> goodsList = goodsMapper.selectByIds(goodsIds);
        Map<Long, Goods> goodsMap = new HashMap<>();
        if (goodsList != null) for (Goods g : goodsList) goodsMap.put(g.getId(), g);

        StringBuilder sb = new StringBuilder("购物车列表：\n");
        double total = 0;
        for (Cart c : carts) {
            Goods g = goodsMap.get(c.getGoodsId());
            if (g != null) {
                double sub = g.getPrice().doubleValue() * c.getQuantity();
                total += sub;
                sb.append(String.format("- %s | 单价¥%.2f | 数量%d | 小计¥%.2f\n",
                        g.getTitle(), g.getPrice().doubleValue(), c.getQuantity(), sub));
            }
        }
        sb.append(String.format("总计: ¥%.2f", total));
        return sb.toString();
    }

    private String executeGetAddresses() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";
        List<DeliveryAddress> addrs = addressMapper.selectByUserId(userId);
        if (addrs == null || addrs.isEmpty()) return "您还没有添加收货地址";
        StringBuilder sb = new StringBuilder("收货地址列表：\n");
        for (DeliveryAddress a : addrs) {
            sb.append(String.format("- ID:%d | %s %s | %s%s%s%s | %s\n",
                    a.getId(), a.getReceiverName(), a.getReceiverPhone(),
                    a.getProvince() != null ? a.getProvince() : "",
                    a.getCity() != null ? a.getCity() : "",
                    a.getDistrict() != null ? a.getDistrict() : "",
                    a.getDetailAddress() != null ? a.getDetailAddress() : "",
                    a.getIsDefault() != null && a.getIsDefault() == 1 ? "[默认]" : ""));
        }
        return sb.toString();
    }

    private String executeGetRatings(Map<String, Object> arguments) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";
        Object sellerIdObj = arguments.get("sellerId");
        Long sellerId = sellerIdObj != null ? Long.valueOf(sellerIdObj.toString()) : userId;

        Double avgRating = ratingMapper.selectAvgRatingBySellerId(sellerId);
        Long count = ratingMapper.selectCountBySellerId(sellerId);
        List<SellerRating> ratings = ratingMapper.selectBySellerId(sellerId, 0, 10);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("卖家评价（ID:%d）：平均评分%.1f星，共%d条评价\n", sellerId, avgRating != null ? avgRating : 0, count != null ? count : 0));
        if (ratings == null || ratings.isEmpty()) {
            sb.append("暂无评价");
        } else {
            for (SellerRating r : ratings) {
                sb.append(String.format("- %d星 | %s\n", r.getRating(), r.getComment() != null ? r.getComment() : "无评论"));
            }
        }
        return sb.toString();
    }

    private String executeGetNotifications() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";
        Long unreadCount = notificationMapper.selectUnreadCount(userId);
        List<Notification> notifications = notificationMapper.selectByUserId(userId, null, 0, 10);
        StringBuilder sb = new StringBuilder(String.format("通知列表（未读%d条）：\n", unreadCount != null ? unreadCount : 0));
        if (notifications == null || notifications.isEmpty()) {
            sb.append("暂无通知");
        } else {
            for (Notification n : notifications) {
                sb.append(String.format("- [%s] %s | %s\n",
                        n.getIsRead() != null && n.getIsRead() == 1 ? "已读" : "未读",
                        n.getTitle(), n.getContent()));
            }
        }
        return sb.toString();
    }

    private String executeGetUnreadMessageCount() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";
        Long count = chatMessageMapper.selectTotalUnreadCount(userId);
        return String.format("您有%d条未读聊天消息", count != null ? count : 0);
    }

    private String executeGetRecentContacts() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";
        List<ChatMessage> contacts = chatMessageMapper.selectRecentContacts(userId);
        if (contacts == null || contacts.isEmpty()) return "暂无聊天记录";
        Set<Long> otherIds = new LinkedHashSet<>();
        for (ChatMessage m : contacts) {
            otherIds.add(m.getSenderId().equals(userId) ? m.getReceiverId() : m.getSenderId());
        }
        List<User> users = userMapper.selectByIds(otherIds);
        Map<Long, User> userMap = new HashMap<>();
        if (users != null) for (User u : users) userMap.put(u.getId(), u);

        Map<Long, Long> unreadMap = new HashMap<>();
        List<Map<String, Object>> unreadList = chatMessageMapper.selectUnreadCountGrouped(userId);
        if (unreadList != null) {
            for (Map<String, Object> row : unreadList) {
                Object sid = row.get("senderId");
                Object cnt = row.get("cnt");
                if (sid != null && cnt != null) {
                    unreadMap.put(((Number) sid).longValue(), ((Number) cnt).longValue());
                }
            }
        }

        StringBuilder sb = new StringBuilder("最近联系人：\n");
        for (Long oid : otherIds) {
            User u = userMap.get(oid);
            Long unread = unreadMap.getOrDefault(oid, 0L);
            sb.append(String.format("- %s（ID:%d）| 未读%d条\n",
                    u != null ? u.getNickname() : "未知用户", oid, unread));
        }
        return sb.toString();
    }

    private String executeGetFollowList(Map<String, Object> arguments) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";
        String type = arguments.get("type") != null ? arguments.get("type").toString() : "following";
        Object targetUserIdObj = arguments.get("userId");
        Long targetUserId = targetUserIdObj != null ? Long.valueOf(targetUserIdObj.toString()) : userId;

        List<Long> ids;
        Long count;
        if ("followers".equalsIgnoreCase(type)) {
            ids = followMapper.selectFollowerIds(targetUserId, 0, 20);
            count = followMapper.selectFollowerCount(targetUserId);
        } else {
            ids = followMapper.selectFollowingIds(targetUserId, 0, 20);
            count = followMapper.selectFollowingCount(targetUserId);
        }
        if (ids == null || ids.isEmpty()) return String.format("%s列表为空", "followers".equalsIgnoreCase(type) ? "粉丝" : "关注");

        List<User> users = userMapper.selectByIds(ids);
        Map<Long, User> userMap = new HashMap<>();
        if (users != null) for (User u : users) userMap.put(u.getId(), u);

        StringBuilder sb = new StringBuilder(String.format("%s列表（共%d人）：\n", "followers".equalsIgnoreCase(type) ? "粉丝" : "关注", count != null ? count : 0));
        for (Long id : ids) {
            User u = userMap.get(id);
            sb.append(String.format("- %s（ID:%d）\n", u != null ? u.getNickname() : "未知用户", id));
        }
        return sb.toString();
    }

    private String executeGetCategories() {
        List<GoodsCategory> cats = categoryMapper.selectAll();
        if (cats == null || cats.isEmpty()) return "暂无分类";
        StringBuilder sb = new StringBuilder("商品分类列表：\n");
        for (GoodsCategory c : cats) {
            sb.append(String.format("- ID:%d | %s | 状态:%s\n", c.getId(), c.getCategoryName(),
                    c.getStatus() != null && c.getStatus() == 1 ? "正常" : "禁用"));
        }
        return sb.toString();
    }

    private String executeGetOrderFundLogs(Map<String, Object> arguments) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";
        Object idObj = arguments.get("orderId");
        if (idObj == null) return "请提供订单ID";
        Long orderId = Long.valueOf(idObj.toString());
        Order order = orderMapper.selectById(orderId);
        if (order == null) return "订单不存在";
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) return "无权查看此订单";

        List<FundLog> logs = fundLogMapper.selectByOrderId(orderId);
        if (logs == null || logs.isEmpty()) return "该订单暂无资金流水";
        StringBuilder sb = new StringBuilder("订单资金流水：\n");
        for (FundLog l : logs) {
            sb.append(String.format("- 类型:%s | 金额:¥%.2f | 状态:%s | 流水号:%s | 时间:%s\n",
                    l.getType(), l.getAmount() != null ? l.getAmount().doubleValue() : 0,
                    l.getStatus(), l.getTradeNo() != null ? l.getTradeNo() : "无", l.getCreateTime()));
        }
        return sb.toString();
    }

    private String executeGetAnnouncements() {
        List<Announcement> anns = announcementMapper.selectActive();
        if (anns == null || anns.isEmpty()) return "暂无系统公告";
        StringBuilder sb = new StringBuilder("系统公告：\n");
        for (Announcement a : anns) {
            sb.append(String.format("- %s\n  %s\n", a.getTitle(), a.getContent()));
        }
        return sb.toString();
    }

    private String executeCancelOrder(Map<String, Object> arguments) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";
        Object idObj = arguments.get("orderId");
        if (idObj == null) return "请提供订单ID";
        Long orderId = Long.valueOf(idObj.toString());
        String reason = arguments.get("reason") != null ? arguments.get("reason").toString() : "用户取消";
        var result = orderService.cancelOrder(userId, orderId, reason);
        return result.getCode() == 200 ? "订单取消成功" : "取消失败: " + result.getMessage();
    }

    private String executeConfirmReceipt(Map<String, Object> arguments) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";
        Object idObj = arguments.get("orderId");
        if (idObj == null) return "请提供订单ID";
        Long orderId = Long.valueOf(idObj.toString());
        var result = orderService.finishOrder(userId, orderId);
        return result.getCode() == 200 ? "确认收货成功" : "操作失败: " + result.getMessage();
    }

    private String executeShipOrder(Map<String, Object> arguments) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";
        Object idObj = arguments.get("orderId");
        Object trackingObj = arguments.get("trackingNo");
        if (idObj == null || trackingObj == null) return "请提供订单ID和快递单号";
        Long orderId = Long.valueOf(idObj.toString());
        String trackingNo = trackingObj.toString();
        var result = orderService.shipOrder(userId, orderId, trackingNo);
        return result.getCode() == 200 ? "发货成功，快递单号: " + trackingNo : "发货失败: " + result.getMessage();
    }

    private String executeRequestRefund(Map<String, Object> arguments) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";
        Object idObj = arguments.get("orderId");
        if (idObj == null) return "请提供订单ID";
        Long orderId = Long.valueOf(idObj.toString());
        String reason = arguments.get("reason") != null ? arguments.get("reason").toString() : "用户申请退款";
        var result = orderService.refundOrder(userId, orderId, reason);
        return result.getCode() == 200 ? "退款申请已提交" : "申请失败: " + result.getMessage();
    }

    private String executeRateOrder(Map<String, Object> arguments) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";
        Object idObj = arguments.get("orderId");
        Object ratingObj = arguments.get("rating");
        if (idObj == null || ratingObj == null) return "请提供订单ID和评分";
        Long orderId = Long.valueOf(idObj.toString());
        Integer rating = Integer.valueOf(ratingObj.toString());
        String comment = arguments.get("comment") != null ? arguments.get("comment").toString() : "";
        var result = orderService.rateOrder(userId, orderId, rating, comment);
        return result.getCode() == 200 ? "评价成功" : "评价失败: " + result.getMessage();
    }

    private String executeToggleFavorite(Map<String, Object> arguments) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";
        Object idObj = arguments.get("goodsId");
        if (idObj == null) return "请提供商品ID";
        Long goodsId = Long.valueOf(idObj.toString());
        GoodsFavorite existing = favoriteMapper.selectByUserAndGoods(userId, goodsId);
        if (existing != null) {
            goodsService.unfavoriteGoods(userId, goodsId);
            return "已取消收藏";
        } else {
            goodsService.favoriteGoods(userId, goodsId);
            return "收藏成功";
        }
    }

    private String executeAddToCart(Map<String, Object> arguments) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";
        Object idObj = arguments.get("goodsId");
        if (idObj == null) return "请提供商品ID";
        Long goodsId = Long.valueOf(idObj.toString());
        Integer quantity = arguments.get("quantity") != null ? Integer.valueOf(arguments.get("quantity").toString()) : 1;

        Cart existing = cartMapper.selectByUserAndGoods(userId, goodsId);
        if (existing != null) {
            cartMapper.updateQuantity(existing.getId(), existing.getQuantity() + quantity);
            return "购物车中该商品数量已更新为" + (existing.getQuantity() + quantity);
        }
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setGoodsId(goodsId);
        cart.setQuantity(quantity);
        cartMapper.insert(cart);
        return "已加入购物车";
    }

    private String executeToggleFollowUser(Map<String, Object> arguments) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";
        Object idObj = arguments.get("userId");
        if (idObj == null) return "请提供目标用户ID";
        Long targetId = Long.valueOf(idObj.toString());
        if (targetId.equals(userId)) return "不能关注自己";
        UserFollow existing = followMapper.selectByFollowerAndFollowing(userId, targetId);
        if (existing != null) {
            followMapper.deleteByFollowerAndFollowing(userId, targetId);
            return "已取消关注";
        } else {
            UserFollow follow = new UserFollow();
            follow.setFollowerId(userId);
            follow.setFollowingId(targetId);
            followMapper.insert(follow);
            return "关注成功";
        }
    }

    private String executeOnlineOfflineGoods(Map<String, Object> arguments) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";
        Object idObj = arguments.get("goodsId");
        Object actionObj = arguments.get("action");
        if (idObj == null || actionObj == null) return "请提供商品ID和操作类型";
        Long goodsId = Long.valueOf(idObj.toString());
        String action = actionObj.toString();
        com.campustrade.common.Result<?> result;
        if ("online".equalsIgnoreCase(action)) {
            result = goodsService.onlineGoods(userId, goodsId);
            return result.getCode() == 200 ? "商品已上架" : "上架失败: " + result.getMessage();
        } else {
            result = goodsService.offlineGoods(userId, goodsId);
            return result.getCode() == 200 ? "商品已下架" : "下架失败: " + result.getMessage();
        }
    }

    private String executeAddAddress(Map<String, Object> arguments) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";

        Object nameObj = arguments.get("receiverName");
        Object phoneObj = arguments.get("receiverPhone");
        if (nameObj == null || nameObj.toString().trim().isEmpty()) return "请提供收货人姓名";
        if (phoneObj == null || phoneObj.toString().trim().isEmpty()) return "请提供收货人手机号";
        String phone = phoneObj.toString().trim();
        if (!phone.matches("1[3-9]\\d{9}")) return "手机号格式不正确，应为11位数字";

        DeliveryAddress addr = new DeliveryAddress();
        addr.setUserId(userId);
        addr.setReceiverName(nameObj.toString().trim());
        addr.setReceiverPhone(phone);
        addr.setProvince(arguments.get("province") != null ? arguments.get("province").toString() : "");
        addr.setCity(arguments.get("city") != null ? arguments.get("city").toString() : "");
        addr.setDistrict(arguments.get("district") != null ? arguments.get("district").toString() : "");
        String detail = arguments.get("detailAddress") != null ? arguments.get("detailAddress").toString() : "";
        if (detail.length() > 200) return "详细地址过长（最多200字符）";
        addr.setDetailAddress(detail);
        addr.setIsDefault(0);
        addressMapper.insert(addr);
        return "收货地址添加成功，ID: " + addr.getId();
    }

    private String executeSubmitReport(Map<String, Object> arguments) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return "请先登录";
        Report report = new Report();
        report.setReporterId(userId);
        String targetType = arguments.get("targetType").toString();
        report.setTargetType("goods".equalsIgnoreCase(targetType) ? 1 : 2);
        report.setTargetId(Long.valueOf(arguments.get("targetId").toString()));
        report.setReason(arguments.get("reason").toString());
        report.setDescription(arguments.get("description") != null ? arguments.get("description").toString() : "");
        report.setStatus("PENDING");
        reportMapper.insert(report);
        return "举报已提交，我们会尽快处理";
    }

    private String executeAdminDashboard() {
        if (!isAdmin()) return "无权限：仅管理员可用";
        Long userCount = userMapper.selectCount(null, null);
        Long goodsCount = goodsMapper.selectCountAll();
        Long orderCount = orderMapper.selectCountAll(null, null, null, null);
        Long pendingAudit = goodsMapper.selectCountByStatus("PENDING_AUDIT");
        Long todayUsers = userMapper.selectCountToday();
        Long todayOrders = orderMapper.selectCountToday();
        Long pendingPay = orderMapper.selectCountByStatus("PENDING_PAY");
        Long paid = orderMapper.selectCountByStatus("PAID");
        Long shipped = orderMapper.selectCountByStatus("SHIPPED");
        Long completed = orderMapper.selectCountByStatus("COMPLETED");
        Long cancelled = orderMapper.selectCountByStatus("CANCELLED");

        return String.format("平台数据概览：\n- 总用户: %d（今日新增%d）\n- 总商品: %d（待审核%d）\n- 总订单: %d（今日新增%d）\n- 订单状态分布: 待支付%d | 已支付%d | 已发货%d | 已完成%d | 已取消%d",
                userCount != null ? userCount : 0, todayUsers != null ? todayUsers : 0,
                goodsCount != null ? goodsCount : 0, pendingAudit != null ? pendingAudit : 0,
                orderCount != null ? orderCount : 0, todayOrders != null ? todayOrders : 0,
                pendingPay != null ? pendingPay : 0, paid != null ? paid : 0,
                shipped != null ? shipped : 0, completed != null ? completed : 0,
                cancelled != null ? cancelled : 0);
    }

    private String executeAdminListUsers(Map<String, Object> arguments) {
        if (!isAdmin()) return "无权限：仅管理员可用";
        String keyword = arguments.get("keyword") != null ? arguments.get("keyword").toString() : null;
        Integer status = arguments.get("status") != null ? Integer.valueOf(arguments.get("status").toString()) : null;
        List<User> users = userMapper.selectList(keyword, status, 0, 20);
        if (users == null || users.isEmpty()) return "未找到用户";
        StringBuilder sb = new StringBuilder("用户列表：\n");
        for (User u : users) {
            sb.append(String.format("- ID:%d | %s(%s) | 手机:%s | 状态:%s\n",
                    u.getId(), u.getNickname(), u.getUsername(),
                    u.getPhone() != null ? u.getPhone() : "无",
                    u.getStatus() != null && u.getStatus() == 1 ? "正常" : "封禁"));
        }
        return sb.toString();
    }

    private String executeAdminBanUser(Map<String, Object> arguments) {
        if (!isAdmin()) return "无权限：仅管理员可用";
        Object idObj = arguments.get("userId");
        Object actionObj = arguments.get("action");
        if (idObj == null || actionObj == null) return "请提供用户ID和操作类型";
        Long userId = Long.valueOf(idObj.toString());
        String action = actionObj.toString();
        com.campustrade.common.Result<?> result;
        if ("ban".equalsIgnoreCase(action)) {
            result = userService.banUser(userId);
            return result.getCode() == 200 ? "用户已封禁" : "封禁失败: " + result.getMessage();
        } else {
            result = userService.unbanUser(userId);
            return result.getCode() == 200 ? "用户已解封" : "解封失败: " + result.getMessage();
        }
    }

    private String executeAdminAuditGoods(Map<String, Object> arguments) {
        if (!isAdmin()) return "无权限：仅管理员可用";
        Object idObj = arguments.get("goodsId");
        Object actionObj = arguments.get("action");
        if (idObj == null || actionObj == null) return "请提供商品ID和操作类型";
        Long goodsId = Long.valueOf(idObj.toString());
        String action = actionObj.toString();
        String reason = arguments.get("reason") != null ? arguments.get("reason").toString() : "";
        String status = "pass".equalsIgnoreCase(action) ? "ONLINE" : "REJECTED";
        var result = goodsService.auditGoods(goodsId, status, reason);
        return result.getCode() == 200 ? ("pass".equalsIgnoreCase(action) ? "商品审核通过" : "商品已拒绝") : "审核失败: " + result.getMessage();
    }

    private String executeAdminListReports(Map<String, Object> arguments) {
        if (!isAdmin()) return "无权限：仅管理员可用";
        String status = arguments.get("status") != null ? arguments.get("status").toString() : null;
        List<Report> reports = reportMapper.selectAllByStatus(null, status, 0, 20);
        if (reports == null || reports.isEmpty()) return "暂无举报记录";
        StringBuilder sb = new StringBuilder("举报列表：\n");
        for (Report r : reports) {
            sb.append(String.format("- ID:%d | 目标类型:%s | 目标ID:%d | 原因:%s | 状态:%s\n",
                    r.getId(), r.getTargetType() == 1 ? "商品" : "用户", r.getTargetId(),
                    r.getReason(), r.getStatus()));
        }
        return sb.toString();
    }

    private String executeAdminHandleRefund(Map<String, Object> arguments) {
        if (!isAdmin()) return "无权限：仅管理员可用";
        Object idObj = arguments.get("orderId");
        Object actionObj = arguments.get("action");
        if (idObj == null || actionObj == null) return "请提供订单ID和操作类型";
        Long orderId = Long.valueOf(idObj.toString());
        String action = actionObj.toString();
        com.campustrade.common.Result<?> result;
        if ("approve".equalsIgnoreCase(action)) {
            result = orderService.adminApproveRefund(orderId);
            return result.getCode() == 200 ? "退款已同意" : "操作失败: " + result.getMessage();
        } else {
            String reason = arguments.get("reason") != null ? arguments.get("reason").toString() : "";
            result = orderService.adminRejectRefund(orderId, reason);
            return result.getCode() == 200 ? "退款已拒绝" : "操作失败: " + result.getMessage();
        }
    }

    private String formatOrder(Order order, Long currentUserId) {
        String statusDesc = STATUS_MAP.getOrDefault(order.getStatus(), order.getStatus());
        String role = order.getBuyerId().equals(currentUserId) ? "买家" : "卖家";
        return String.format("订单号: %s | 角色: %s | 状态: %s | 金额: ¥%.2f | 创建时间: %s",
                order.getOrderNo(), role, statusDesc,
                order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0,
                order.getCreateTime());
    }

    private String formatGoods(Goods g) {
        return String.format("商品ID:%d | %s | ¥%.2f | 库存%d | 状态:%s",
                g.getId(), g.getTitle(),
                g.getPrice() != null ? g.getPrice().doubleValue() : 0,
                g.getStock(),
                GOODS_STATUS_MAP.getOrDefault(g.getStatus(), g.getStatus()));
    }
}
