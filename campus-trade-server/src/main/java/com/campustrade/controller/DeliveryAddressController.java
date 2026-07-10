package com.campustrade.controller;

import com.campustrade.common.Result;
import com.campustrade.entity.DeliveryAddress;
import com.campustrade.mapper.DeliveryAddressMapper;
import com.campustrade.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "收货地址接口")
@RestController
@RequestMapping("/api/address")
public class DeliveryAddressController {

    @Autowired
    private DeliveryAddressMapper deliveryAddressMapper;

    @ApiOperation("地址列表")
    @GetMapping
    public Result<List<DeliveryAddress>> list() {
        Long userId = SecurityUtil.requireCurrentUserId();
        return Result.success(deliveryAddressMapper.selectByUserId(userId));
    }

    @ApiOperation("地址详情")
    @GetMapping("/{id}")
    public Result<DeliveryAddress> getById(@PathVariable Long id) {
        DeliveryAddress addr = deliveryAddressMapper.selectById(id);
        if (addr == null || !addr.getUserId().equals(SecurityUtil.requireCurrentUserId())) {
            return Result.error(404, "地址不存在");
        }
        return Result.success(addr);
    }

    @ApiOperation("新增地址")
    @PostMapping
    public Result<Void> add(@RequestBody DeliveryAddress address) {
        Long userId = SecurityUtil.requireCurrentUserId();
        address.setUserId(userId);
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            deliveryAddressMapper.resetDefaultByUserId(userId);
        }
        if (address.getIsDefault() == null) {
            address.setIsDefault(0);
        }
        deliveryAddressMapper.insert(address);
        return Result.success();
    }

    @ApiOperation("修改地址")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody DeliveryAddress address) {
        DeliveryAddress existing = deliveryAddressMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(SecurityUtil.requireCurrentUserId())) {
            return Result.error(404, "地址不存在");
        }
        address.setId(id);
        address.setVersion(existing.getVersion());
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            deliveryAddressMapper.resetDefaultByUserId(existing.getUserId());
        }
        deliveryAddressMapper.updateById(address);
        return Result.success();
    }

    @ApiOperation("删除地址")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        DeliveryAddress existing = deliveryAddressMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(SecurityUtil.requireCurrentUserId())) {
            return Result.error(404, "地址不存在");
        }
        deliveryAddressMapper.deleteById(id);
        return Result.success();
    }

    @ApiOperation("设为默认地址")
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        DeliveryAddress existing = deliveryAddressMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(SecurityUtil.requireCurrentUserId())) {
            return Result.error(404, "地址不存在");
        }
        deliveryAddressMapper.resetDefaultByUserId(existing.getUserId());
        DeliveryAddress update = new DeliveryAddress();
        update.setId(id);
        update.setIsDefault(1);
        update.setVersion(existing.getVersion());
        deliveryAddressMapper.updateById(update);
        return Result.success();
    }
}