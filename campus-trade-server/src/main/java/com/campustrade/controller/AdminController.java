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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
