package com.campustrade.service.impl;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.common.ResultCode;
import com.campustrade.constant.MQConstant;
import com.campustrade.dto.ReportCreateDTO;
import com.campustrade.dto.ReportHandleDTO;
import com.campustrade.entity.Report;
import com.campustrade.entity.User;
import com.campustrade.enum_.NotificationType;
import com.campustrade.enum_.ReportStatus;
import com.campustrade.mapper.ReportMapper;
import com.campustrade.mapper.UserMapper;
import com.campustrade.service.ReportService;
import com.campustrade.service.NotificationService;
import com.campustrade.vo.ReportVO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> createReport(Long reporterId, ReportCreateDTO dto) {
        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(dto.getTargetType());
        report.setTargetId(dto.getTargetId());
        report.setReason(dto.getReason());
        report.setDescription(dto.getDescription());
        report.setImages(dto.getImages());
        report.setStatus(ReportStatus.PENDING.getCode());
        reportMapper.insert(report);

        rabbitTemplate.convertAndSend(MQConstant.AUDIT_EXCHANGE, MQConstant.AUDIT_REPORT_KEY, report.getId());
        return Result.success();
    }

    @Override
    public Result<PageResult<ReportVO>> listMyReports(Long reporterId, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Report> list = reportMapper.selectByReporterId(reporterId, offset, pageSize);
        List<ReportVO> vos = toVOList(list);
        Long total = (long) list.size();
        return Result.success(new PageResult<>(vos, total));
    }

    @Override
    public Result<PageResult<ReportVO>> listReports(String status, Integer targetType, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Report> list = reportMapper.selectList(status, targetType, offset, pageSize);
        Long total = reportMapper.selectCount(status, targetType);
        List<ReportVO> vos = toVOList(list);
        return Result.success(new PageResult<>(vos, total));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> handleReport(Long handlerId, Long reportId, ReportHandleDTO dto) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) return Result.error(ResultCode.REPORT_NOT_FOUND);
        report.setStatus(ReportStatus.FINISHED.getCode());
        report.setHandlerId(handlerId);
        report.setHandleResult(dto.getHandleResult());
        report.setHandleTime(LocalDateTime.now());
        int rows = reportMapper.updateById(report);
        if (rows == 0) return Result.error(ResultCode.DATA_VERSION_ERROR);

        notificationService.sendNotification(report.getReporterId(), "举报处理通知",
                "您的举报已处理：" + dto.getHandleResult(), NotificationType.REPORT.getCode(), reportId);
        return Result.success();
    }

    @Override
    public Result<PageResult<ReportVO>> listAllReports(String status, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Report> list = reportMapper.selectAllByStatus(status, offset, pageSize);
        Long total = reportMapper.selectCountByStatus(status);
        List<ReportVO> vos = toVOList(list);
        return Result.success(new PageResult<>(vos, total));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> resolveReport(Long reportId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) return Result.error(ResultCode.REPORT_NOT_FOUND);
        report.setStatus(ReportStatus.RESOLVED.getCode());
        report.setHandleResult("举报成立，已处理");
        report.setHandleTime(LocalDateTime.now());
        int rows = reportMapper.updateById(report);
        if (rows == 0) return Result.error(ResultCode.DATA_VERSION_ERROR);

        notificationService.sendNotification(report.getReporterId(), "举报处理通知",
                "您的举报已成立并处理", NotificationType.REPORT.getCode(), reportId);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> dismissReport(Long reportId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) return Result.error(ResultCode.REPORT_NOT_FOUND);
        report.setStatus(ReportStatus.DISMISSED.getCode());
        report.setHandleResult("举报不成立，已驳回");
        report.setHandleTime(LocalDateTime.now());
        int rows = reportMapper.updateById(report);
        if (rows == 0) return Result.error(ResultCode.DATA_VERSION_ERROR);

        notificationService.sendNotification(report.getReporterId(), "举报处理通知",
                "您的举报经审核不成立，已驳回", NotificationType.REPORT.getCode(), reportId);
        return Result.success();
    }

    private ReportVO toVO(Report report) {
        ReportVO vo = new ReportVO();
        vo.setId(report.getId());
        vo.setReporterId(report.getReporterId());
        vo.setTargetType(report.getTargetType());
        vo.setTargetId(report.getTargetId());
        vo.setReason(report.getReason());
        vo.setDescription(report.getDescription());
        vo.setImages(report.getImages());
        vo.setStatus(report.getStatus());
        vo.setHandlerId(report.getHandlerId());
        vo.setHandleResult(report.getHandleResult());
        vo.setHandleTime(report.getHandleTime());
        vo.setCreateTime(report.getCreateTime());

        User reporter = userMapper.selectById(report.getReporterId());
        if (reporter != null) vo.setReporterName(reporter.getNickname() != null ? reporter.getNickname() : reporter.getUsername());
        if (report.getHandlerId() != null) {
            User handler = userMapper.selectById(report.getHandlerId());
            if (handler != null) vo.setHandlerName(handler.getNickname() != null ? handler.getNickname() : handler.getUsername());
        }
        return vo;
    }

    private List<ReportVO> toVOList(List<Report> reports) {
        if (reports == null || reports.isEmpty()) return List.of();

        Set<Long> userIds = new HashSet<>();
        for (Report r : reports) {
            userIds.add(r.getReporterId());
            if (r.getHandlerId() != null) userIds.add(r.getHandlerId());
        }
        Map<Long, User> userMap = userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return reports.stream().map(report -> {
            ReportVO vo = new ReportVO();
            vo.setId(report.getId());
            vo.setReporterId(report.getReporterId());
            vo.setTargetType(report.getTargetType());
            vo.setTargetId(report.getTargetId());
            vo.setReason(report.getReason());
            vo.setDescription(report.getDescription());
            vo.setImages(report.getImages());
            vo.setStatus(report.getStatus());
            vo.setHandlerId(report.getHandlerId());
            vo.setHandleResult(report.getHandleResult());
            vo.setHandleTime(report.getHandleTime());
            vo.setCreateTime(report.getCreateTime());

            User reporter = userMap.get(report.getReporterId());
            if (reporter != null) vo.setReporterName(reporter.getNickname() != null ? reporter.getNickname() : reporter.getUsername());
            if (report.getHandlerId() != null) {
                User handler = userMap.get(report.getHandlerId());
                if (handler != null) vo.setHandlerName(handler.getNickname() != null ? handler.getNickname() : handler.getUsername());
            }
            return vo;
        }).collect(Collectors.toList());
    }
}