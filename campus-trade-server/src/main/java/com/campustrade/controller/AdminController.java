package com.campustrade.controller;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.common.ResultCode;
import com.campustrade.dto.GoodsAuditDTO;
import com.campustrade.dto.GoodsQueryDTO;
import com.campustrade.service.GoodsService;
import com.campustrade.service.UserService;
import com.campustrade.service.LogService;
import com.campustrade.service.OrderService;
import com.campustrade.service.ReportService;
import com.campustrade.vo.GoodsVO;
import com.campustrade.vo.UserVO;
import com.campustrade.vo.OperationLogVO;
import com.campustrade.vo.SecurityLogVO;
import com.campustrade.vo.OrderVO;
import com.campustrade.vo.ReportVO;
import com.campustrade.vo.AdminInfoVO;
import com.campustrade.mapper.RoleMapper;
import com.campustrade.mapper.PermissionMapper;
import com.campustrade.mapper.GoodsMapper;
import com.campustrade.mapper.OrderMapper;
import com.campustrade.mapper.SecurityLogMapper;
import com.campustrade.mapper.FundLogMapper;
import com.campustrade.entity.FundLog;
import com.campustrade.entity.User;
import com.campustrade.entity.Role;
import com.campustrade.mapper.UserMapper;
import com.campustrade.enum_.OrderStatus;
import com.campustrade.enum_.GoodsStatus;
import com.campustrade.service.NotificationService;
import com.campustrade.service.SystemConfigService;
import com.campustrade.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Api(tags = "管理员接口")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private LogService logService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private SecurityLogMapper securityLogMapper;

    @Autowired
    private FundLogMapper fundLogMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private com.campustrade.service.EmailService emailService;

    @ApiOperation("获取当前管理员信息")
    @GetMapping("/info")
    public Result<AdminInfoVO> getAdminInfo() {
        Long userId = SecurityUtil.requireCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        AdminInfoVO vo = new AdminInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        List<Role> roles = roleMapper.selectByUserId(userId);
        vo.setRoles(roles.stream().map(Role::getRoleCode).collect(Collectors.toList()));
        List<String> permCodes = permissionMapper.selectPermissionCodesByUserId(userId);
        vo.setPermissions(permCodes);
        return Result.success(vo);
    }

    @ApiOperation("仪表盘统计")
    @GetMapping("/dashboard/stats")
    public Result<Map<String, Object>> dashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userService.countUsers());
        stats.put("goodsCount", goodsService.countGoods());
        stats.put("orderCount", orderService.countOrders());
        stats.put("pendingAudit", goodsService.countPendingAudit());
        Map<String, Long> goodsStatusMap = new HashMap<>();
        String[] goodsStatuses = {"ONLINE", "OFFLINE", "SOLD", "PENDING", "DRAFT", "REJECTED"};
        for (String s : goodsStatuses) {
            Long count = goodsMapper.selectCountByStatus(s);
            if (count != null && count > 0) goodsStatusMap.put(s, count);
        }
        stats.put("goodsStatusMap", goodsStatusMap);
        Map<String, Long> orderStatusMap = new HashMap<>();
        String[] orderStatuses = {"PENDING_PAY", "PAID", "SHIPPING", "PENDING_REVIEW", "FINISHED", "CANCELLED", "REFUND"};
        for (String s : orderStatuses) {
            Long count = orderMapper.selectCountByStatus(s);
            if (count != null && count > 0) orderStatusMap.put(s, count);
        }
        stats.put("orderStatusMap", orderStatusMap);
        Long todayLogin = securityLogMapper.selectCountTodayByEventType("LOGIN_SUCCESS");
        stats.put("todayActive", todayLogin != null ? todayLogin : 0L);
        Long todayUsers = userMapper.selectCountToday();
        stats.put("todayNewUsers", todayUsers != null ? todayUsers : 0L);
        Long todayOrders = orderMapper.selectCountToday();
        stats.put("todayOrders", todayOrders != null ? todayOrders : 0L);
        Long bannedUsers = userMapper.selectCount(null, 0);
        stats.put("bannedUsers", bannedUsers != null ? bannedUsers : 0L);
        return Result.success(stats);
    }

    @ApiOperation("用户列表")
    @GetMapping("/user")
    public Result<PageResult<UserVO>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return userService.listUsers(keyword, status, pageNum, pageSize);
    }

    @ApiOperation("封禁用户")
    @PutMapping("/user/{id}/ban")
    public Result<Void> banUser(@PathVariable Long id) {
        return userService.banUser(id);
    }

    @ApiOperation("解封用户")
    @PutMapping("/user/{id}/unban")
    public Result<Void> unbanUser(@PathVariable Long id) {
        return userService.unbanUser(id);
    }

    @ApiOperation("商品审核列表")
    @GetMapping("/goods")
    public Result<PageResult<GoodsVO>> listGoods(GoodsQueryDTO dto) {
        return goodsService.listGoodsByAdmin(dto);
    }

    @ApiOperation("审核商品")
    @PutMapping("/goods/{id}/audit")
    public Result<Void> auditGoods(@PathVariable Long id, @Validated @RequestBody GoodsAuditDTO dto) {
        return goodsService.auditGoods(id, dto.getStatus(), dto.getRejectReason());
    }

    @ApiOperation("订单管理列表")
    @GetMapping("/order")
    public Result<PageResult<OrderVO>> listOrders(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return orderService.listAllOrders(orderNo, status, startDate, endDate, pageNum, pageSize);
    }

    @ApiOperation("管理员同意退款")
    @PutMapping("/order/{id}/approve-refund")
    public Result<Void> approveRefund(@PathVariable Long id) {
        return orderService.adminApproveRefund(id);
    }

    @ApiOperation("管理员拒绝退款")
    @PutMapping("/order/{id}/reject-refund")
    public Result<Void> rejectRefund(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return orderService.adminRejectRefund(id, reason);
    }

    @ApiOperation("举报管理列表")
    @GetMapping("/report")
    public Result<PageResult<ReportVO>> listReports(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return reportService.listAllReports(keyword, status, pageNum, pageSize);
    }

    @ApiOperation("处理举报-通过")
    @PutMapping("/report/{id}/resolve")
    public Result<Void> resolveReport(@PathVariable Long id) {
        return reportService.resolveReport(id);
    }

    @ApiOperation("处理举报-驳回")
    @PutMapping("/report/{id}/dismiss")
    public Result<Void> dismissReport(@PathVariable Long id) {
        return reportService.dismissReport(id);
    }

    @ApiOperation("操作日志")
    @GetMapping("/log/operation")
    public Result<PageResult<OperationLogVO>> listOperationLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return logService.listOperationLogs(keyword, pageNum, pageSize);
    }

    @ApiOperation("安全日志")
    @GetMapping("/log/security")
    public Result<PageResult<SecurityLogVO>> listSecurityLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return logService.listSecurityLogs(keyword, pageNum, pageSize);
    }

    @ApiOperation("导出用户CSV")
    @GetMapping("/export/users")
    public void exportUsers(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=users.csv");
        PrintWriter writer = response.getWriter();
        writer.write('\uFEFF');
        writer.println("ID,用户名,昵称,手机号,邮箱,状态,注册时间");
        List<User> users = userMapper.selectList(null, null, 0, 10000);
        for (User u : users) {
            writer.println(u.getId() + "," + csvEscape(u.getUsername()) + "," +
                csvEscape(u.getNickname()) + "," +
                csvEscape(u.getPhone()) + "," +
                csvEscape(u.getEmail()) + "," +
                (u.getStatus() != null ? u.getStatus() : "") + "," +
                (u.getCreateTime() != null ? u.getCreateTime() : ""));
        }
        writer.flush();
    }

    @ApiOperation("导出订单CSV")
    @GetMapping("/export/orders")
    public void exportOrders(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=orders.csv");
        PrintWriter writer = response.getWriter();
        writer.write('\uFEFF');
        writer.println("ID,订单号,买家ID,卖家ID,金额,状态,创建时间");
        List<com.campustrade.entity.Order> orders = orderMapper.selectAll(null, null, null, null, 0, 10000);
        for (com.campustrade.entity.Order o : orders) {
            writer.println(o.getId() + "," + o.getOrderNo() + "," +
                o.getBuyerId() + "," + o.getSellerId() + "," +
                o.getTotalAmount() + "," + o.getStatus() + "," +
                (o.getCreateTime() != null ? o.getCreateTime() : ""));
        }
        writer.flush();
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") ||
            value.startsWith("=") || value.startsWith("+") || value.startsWith("-") || value.startsWith("@")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @ApiOperation("获取系统配置")
    @GetMapping("/system-config")
    public Result<List<com.campustrade.entity.SystemConfig>> listSystemConfig() {
        return systemConfigService.listAll();
    }

    @ApiOperation("更新系统配置")
    @PutMapping("/system-config")
    public Result<Void> updateSystemConfig(@RequestBody List<com.campustrade.entity.SystemConfig> configs) {
        return systemConfigService.batchUpdate(configs);
    }

    @ApiOperation("获取支付宝配置状态")
    @GetMapping("/alipay-status")
    public Result<Map<String, Object>> getAlipayStatus() {
        Map<String, Object> status = new HashMap<>();
        String appId = systemConfigService.getConfigValue("alipay.app_id");
        String privateKey = systemConfigService.getDecryptedValue("alipay.private_key");
        String alipayPublicKey = systemConfigService.getDecryptedValue("alipay.alipay_public_key");
        status.put("configured", appId != null && !appId.isEmpty() && privateKey != null && !privateKey.isEmpty() && alipayPublicKey != null && !alipayPublicKey.isEmpty());
        status.put("appId", appId != null && !appId.isEmpty() ? "已配置" : "未配置");
        status.put("privateKey", privateKey != null && !privateKey.isEmpty() ? "已配置" : "未配置");
        status.put("alipayPublicKey", alipayPublicKey != null && !alipayPublicKey.isEmpty() ? "已配置" : "未配置");
        return Result.success(status);
    }

    @ApiOperation("获取邮件服务配置状态")
    @GetMapping("/email-status")
    public Result<Map<String, Object>> getEmailStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("configured", emailService.isConfigured());
        status.put("host", systemConfigService.getConfigValue("mail.host"));
        status.put("port", systemConfigService.getConfigValue("mail.port"));
        status.put("username", systemConfigService.getConfigValue("mail.username"));
        status.put("from", systemConfigService.getConfigValue("mail.from"));
        return Result.success(status);
    }

    @ApiOperation("资金流水列表")
    @GetMapping("/fund-log")
    public Result<PageResult<FundLog>> listFundLogs(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long orderId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<FundLog> list = fundLogMapper.selectAll(type, orderId, offset, pageSize);
        Long total = fundLogMapper.selectCountAll(type, orderId);
        return Result.success(new PageResult<>(list, total));
    }
}
