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
import com.campustrade.entity.User;
import com.campustrade.entity.Role;
import com.campustrade.mapper.UserMapper;
import com.campustrade.enum_.OrderStatus;
import com.campustrade.enum_.GoodsStatus;
import com.campustrade.service.NotificationService;
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

    private NotificationService notificationService;

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
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return userService.listUsers(username, status, pageNum, pageSize);
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
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return orderService.listAllOrders(status, pageNum, pageSize);
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
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return reportService.listAllReports(status, pageNum, pageSize);
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
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return logService.listOperationLogs(module, username, pageNum, pageSize);
    }

    @ApiOperation("安全日志")
    @GetMapping("/log/security")
    public Result<PageResult<SecurityLogVO>> listSecurityLogs(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return logService.listSecurityLogs(eventType, username, pageNum, pageSize);
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
        List<com.campustrade.entity.Order> orders = orderMapper.selectAll(null, 0, 10000);
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
}
