package com.campustrade.service.impl;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.constant.MQConstant;
import com.campustrade.entity.OperationLog;
import com.campustrade.entity.SecurityLog;
import com.campustrade.mapper.OperationLogMapper;
import com.campustrade.mapper.SecurityLogMapper;
import com.campustrade.service.LogService;
import com.campustrade.vo.OperationLogVO;
import com.campustrade.vo.SecurityLogVO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LogServiceImpl implements LogService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private SecurityLogMapper securityLogMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public Result<PageResult<OperationLogVO>> listOperationLogs(String module, String username, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<OperationLog> list = operationLogMapper.selectList(module, username, offset, pageSize);
        Long total = operationLogMapper.selectCount(module, username);
        List<OperationLogVO> voList = list.stream().map(this::toOperationLogVO).collect(Collectors.toList());
        return Result.success(new PageResult<>(voList, total));
    }

    @Override
    public Result<PageResult<SecurityLogVO>> listSecurityLogs(String eventType, String username, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<SecurityLog> list = securityLogMapper.selectList(eventType, username, offset, pageSize);
        Long total = securityLogMapper.selectCount(eventType, username);
        List<SecurityLogVO> voList = list.stream().map(this::toSecurityLogVO).collect(Collectors.toList());
        return Result.success(new PageResult<>(voList, total));
    }

    @Override
    @Async
    public void recordOperationLog(OperationLog log) {
        rabbitTemplate.convertAndSend(MQConstant.LOG_EXCHANGE, MQConstant.LOG_RECORD_KEY, log);
        operationLogMapper.insert(log);
    }

    @Override
    @Async
    public void recordSecurityLog(SecurityLog log) {
        securityLogMapper.insert(log);
    }

    private OperationLogVO toOperationLogVO(OperationLog log) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(log.getId());
        vo.setUserId(log.getUserId());
        vo.setUsername(log.getUsername());
        vo.setModule(log.getModule());
        vo.setOperation(log.getOperation());
        vo.setMethod(log.getMethod());
        vo.setRequestUrl(log.getRequestUrl());
        vo.setIp(log.getIp());
        vo.setDuration(log.getDuration());
        vo.setStatus(log.getStatus());
        vo.setErrorMsg(log.getErrorMsg());
        vo.setTraceId(log.getTraceId());
        vo.setCreateTime(log.getCreateTime() != null ? log.getCreateTime().toString() : null);
        return vo;
    }

    private SecurityLogVO toSecurityLogVO(SecurityLog log) {
        SecurityLogVO vo = new SecurityLogVO();
        vo.setId(log.getId());
        vo.setUserId(log.getUserId());
        vo.setUsername(log.getUsername());
        vo.setEventType(log.getEventType());
        vo.setIp(log.getIp());
        vo.setDetail(log.getDetail());
        vo.setTraceId(log.getTraceId());
        vo.setCreateTime(log.getCreateTime() != null ? log.getCreateTime().toString() : null);
        return vo;
    }
}
