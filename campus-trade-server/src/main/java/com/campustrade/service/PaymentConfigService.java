package com.campustrade.service;

import com.campustrade.common.Result;
import com.campustrade.vo.PaymentConfigVO;

import java.util.List;

public interface PaymentConfigService {

    Result<List<PaymentConfigVO>> listByUserId(Long userId);

    Result<PaymentConfigVO> getDefault(Long userId);

    Result<PaymentConfigVO> create(Long userId, String paymentType, String alipayAccount, String realName, Integer isDefault);

    Result<PaymentConfigVO> update(Long userId, Long id, String alipayAccount, String realName, Integer isDefault);

    Result<Void> delete(Long userId, Long id);

    Result<Void> setDefault(Long userId, Long id);
}