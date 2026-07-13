package com.campustrade.service;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.dto.ReportCreateDTO;
import com.campustrade.dto.ReportHandleDTO;
import com.campustrade.vo.ReportVO;

public interface ReportService {

    Result<Void> createReport(Long reporterId, ReportCreateDTO dto);

    Result<PageResult<ReportVO>> listMyReports(Long reporterId, Integer pageNum, Integer pageSize);

    Result<PageResult<ReportVO>> listReports(String status, Integer targetType, Integer pageNum, Integer pageSize);

    Result<Void> handleReport(Long handlerId, Long reportId, ReportHandleDTO dto);

    Result<PageResult<ReportVO>> listAllReports(String keyword, String status, Integer pageNum, Integer pageSize);

    Result<Void> resolveReport(Long reportId);

    Result<Void> dismissReport(Long reportId);
}