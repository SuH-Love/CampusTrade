package com.campustrade.controller;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.dto.ReportCreateDTO;
import com.campustrade.dto.ReportHandleDTO;
import com.campustrade.service.ReportService;
import com.campustrade.util.SecurityUtil;
import com.campustrade.vo.ReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Api(tags = "举报接口")
@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @ApiOperation("提交举报")
    @PostMapping
    public Result<Void> createReport(@Validated @RequestBody ReportCreateDTO dto) {
        return reportService.createReport(SecurityUtil.requireCurrentUserId(), dto);
    }

    @ApiOperation("我的举报")
    @GetMapping("/mine")
    public Result<PageResult<ReportVO>> listMyReports(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return reportService.listMyReports(SecurityUtil.requireCurrentUserId(), pageNum, pageSize);
    }

    @ApiOperation("处理举报(管理员)")
    @PutMapping("/{id}/handle")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Void> handleReport(@PathVariable Long id, @Validated @RequestBody ReportHandleDTO dto) {
        return reportService.handleReport(SecurityUtil.requireCurrentUserId(), id, dto);
    }
}
