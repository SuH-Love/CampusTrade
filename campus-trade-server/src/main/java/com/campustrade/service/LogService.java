package com.campustrade.service;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.entity.OperationLog;
import com.campustrade.entity.SecurityLog;
import com.campustrade.vo.OperationLogVO;
import com.campustrade.vo.SecurityLogVO;

public interface LogService {

    Result<PageResult<OperationLogVO>> listOperationLogs(String keyword, Integer pageNum, Integer pageSize);

    Result<PageResult<SecurityLogVO>> listSecurityLogs(String keyword, Integer pageNum, Integer pageSize);

    void recordOperationLog(OperationLog log);

    void recordSecurityLog(SecurityLog log);
}
