package com.campustrade.controller;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
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
import com.campustrade.entity.User;
import com.campustrade.entity.Role;
import com.campustrade.entity.Order;
import com.campustrade.entity.OrderItem;
import com.campustrade.entity.Goods;
import com.campustrade.mapper.UserMapper;
import com.campustrade.mapper.OrderMapper;
import com.campustrade.mapper.OrderItemMapper;
import com.campustrade.mapper.GoodsMapper;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        Order order = orderMapper.selectById(id);
        if (order == null) return Result.error(ResultCode.ORDER_NOT_FOUND);
        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setCancelTime(java.time.LocalDateTime.now());
        orderMapper.updateById(order);
        List<OrderItem> items = orderItemMapper.selectByOrderId(id);
        for (OrderItem item : items) {
            Goods goods = goodsMapper.selectById(item.getGoodsId());
            if (goods != null) {
                goods.setStatus(GoodsStatus.ONLINE.getCode());
                goodsMapper.updateById(goods);
            }
        }
        notificationService.sendNotification(order.getBuyerId(), "退款成功",
                "管理员同意了订单「" + order.getOrderNo() + "」的退款", "ORDER", order.getId());
        notificationService.sendNotification(order.getSellerId(), "退款通知",
                "管理员同意了订单「" + order.getOrderNo() + "」的退款", "ORDER", order.getId());
        return Result.success();
    }

    @ApiOperation("管理员拒绝退款")
    @PutMapping("/order/{id}/reject-refund")
    public Result<Void> rejectRefund(@PathVariable Long id, @RequestParam(required = false) String reason) {
        Order order = orderMapper.selectById(id);
        if (order == null) return Result.error(ResultCode.ORDER_NOT_FOUND);
        order.setStatus(OrderStatus.PAID.getCode());
        order.setCancelReason(reason);
        orderMapper.updateById(order);
        notificationService.sendNotification(order.getBuyerId(), "退款被拒绝",
                "管理员拒绝了订单「" + order.getOrderNo() + "」的退款", "ORDER", order.getId());
        notificationService.sendNotification(order.getSellerId(), "退款通知",
                "管理员拒绝了订单「" + order.getOrderNo() + "」的退款", "ORDER", order.getId());
        return Result.success();
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
}
