package com.campustrade.service.impl;

import com.campustrade.common.Result;
import com.campustrade.common.ResultCode;
import com.campustrade.entity.PaymentConfig;
import com.campustrade.mapper.PaymentConfigMapper;
import com.campustrade.service.PaymentConfigService;
import com.campustrade.vo.PaymentConfigVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PaymentConfigServiceImpl implements PaymentConfigService {

    @Autowired
    private PaymentConfigMapper paymentConfigMapper;

    @Override
    public Result<List<PaymentConfigVO>> listByUserId(Long userId) {
        List<PaymentConfig> list = paymentConfigMapper.selectByUserId(userId);
        List<PaymentConfigVO> vos = list.stream().map(this::toVO).collect(Collectors.toList());
        return Result.success(vos);
    }

    @Override
    public Result<PaymentConfigVO> getDefault(Long userId) {
        PaymentConfig config = paymentConfigMapper.selectDefaultByUserId(userId);
        return Result.success(config != null ? toVO(config) : null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<PaymentConfigVO> create(Long userId, String paymentType, String alipayAccount, String realName, Integer isDefault) {
        if (alipayAccount == null || alipayAccount.trim().isEmpty()) {
            return Result.error(400, "支付宝账号不能为空");
        }
        if (realName == null || realName.trim().isEmpty()) {
            return Result.error(400, "真实姓名不能为空");
        }
        if (isDefault != null && isDefault == 1) {
            paymentConfigMapper.resetDefaultByUserId(userId);
        }
        PaymentConfig config = new PaymentConfig();
        config.setUserId(userId);
        config.setPaymentType(paymentType != null ? paymentType : "ALIPAY");
        config.setAlipayAccount(alipayAccount.trim());
        config.setRealName(realName.trim());
        config.setIsDefault(isDefault != null ? isDefault : 0);
        config.setStatus("ACTIVE");
        paymentConfigMapper.insert(config);
        return Result.success(toVO(config));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<PaymentConfigVO> update(Long userId, Long id, String alipayAccount, String realName, Integer isDefault) {
        PaymentConfig config = paymentConfigMapper.selectById(id);
        if (config == null || !config.getUserId().equals(userId)) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        if (alipayAccount != null) config.setAlipayAccount(alipayAccount.trim());
        if (realName != null) config.setRealName(realName.trim());
        if (isDefault != null && isDefault == 1) {
            paymentConfigMapper.resetDefaultByUserId(userId);
            config.setIsDefault(1);
        }
        int rows = paymentConfigMapper.updateById(config);
        if (rows == 0) return Result.error(ResultCode.DATA_VERSION_ERROR);
        return Result.success(toVO(config));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long userId, Long id) {
        paymentConfigMapper.logicDeleteById(id, userId);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> setDefault(Long userId, Long id) {
        PaymentConfig config = paymentConfigMapper.selectById(id);
        if (config == null || !config.getUserId().equals(userId)) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        paymentConfigMapper.resetDefaultByUserId(userId);
        config.setIsDefault(1);
        paymentConfigMapper.updateById(config);
        return Result.success();
    }

    private PaymentConfigVO toVO(PaymentConfig config) {
        PaymentConfigVO vo = new PaymentConfigVO();
        vo.setId(config.getId());
        vo.setUserId(config.getUserId());
        vo.setPaymentType(config.getPaymentType());
        vo.setAlipayAccount(config.getAlipayAccount());
        vo.setRealName(config.getRealName());
        vo.setIsDefault(config.getIsDefault());
        vo.setStatus(config.getStatus());
        vo.setCreateTime(config.getCreateTime());
        return vo;
    }
}