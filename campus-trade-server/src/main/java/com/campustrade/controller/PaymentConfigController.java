package com.campustrade.controller;

import com.campustrade.common.Result;
import com.campustrade.service.PaymentConfigService;
import com.campustrade.util.SecurityUtil;
import com.campustrade.vo.PaymentConfigVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "收款配置接口")
@RestController
@RequestMapping("/api/payment-config")
public class PaymentConfigController {

    @Autowired
    private PaymentConfigService paymentConfigService;

    @ApiOperation("获取我的收款配置列表")
    @GetMapping
    public Result<List<PaymentConfigVO>> list() {
        return paymentConfigService.listByUserId(SecurityUtil.requireCurrentUserId());
    }

    @ApiOperation("获取默认收款配置")
    @GetMapping("/default")
    public Result<PaymentConfigVO> getDefault() {
        return paymentConfigService.getDefault(SecurityUtil.requireCurrentUserId());
    }

    @ApiOperation("添加收款配置")
    @PostMapping
    public Result<PaymentConfigVO> create(
            @RequestParam String alipayAccount,
            @RequestParam String realName,
            @RequestParam(required = false, defaultValue = "0") Integer isDefault) {
        return paymentConfigService.create(SecurityUtil.requireCurrentUserId(), "ALIPAY", alipayAccount, realName, isDefault);
    }

    @ApiOperation("修改收款配置")
    @PutMapping("/{id}")
    public Result<PaymentConfigVO> update(
            @PathVariable Long id,
            @RequestParam(required = false) String alipayAccount,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Integer isDefault) {
        return paymentConfigService.update(SecurityUtil.requireCurrentUserId(), id, alipayAccount, realName, isDefault);
    }

    @ApiOperation("删除收款配置")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return paymentConfigService.delete(SecurityUtil.requireCurrentUserId(), id);
    }

    @ApiOperation("设为默认")
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        return paymentConfigService.setDefault(SecurityUtil.requireCurrentUserId(), id);
    }
}