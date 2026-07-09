package com.campustrade.controller;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.dto.GoodsCreateDTO;
import com.campustrade.dto.GoodsQueryDTO;
import com.campustrade.dto.GoodsUpdateDTO;
import com.campustrade.service.GoodsService;
import com.campustrade.util.SecurityUtil;
import com.campustrade.vo.GoodsVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Api(tags = "商品接口")
@RestController
@RequestMapping("/api/goods")
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    @ApiOperation("发布商品")
    @PostMapping
    public Result<GoodsVO> createGoods(@Validated @RequestBody GoodsCreateDTO dto) {
        return goodsService.createGoods(SecurityUtil.requireCurrentUserId(), dto);
    }

    @ApiOperation("修改商品")
    @PutMapping("/{id}")
    public Result<GoodsVO> updateGoods(@PathVariable Long id, @Validated @RequestBody GoodsUpdateDTO dto) {
        return goodsService.updateGoods(SecurityUtil.requireCurrentUserId(), id, dto);
    }

    @ApiOperation("删除商品")
    @DeleteMapping("/{id}")
    public Result<Void> deleteGoods(@PathVariable Long id) {
        return goodsService.deleteGoods(SecurityUtil.requireCurrentUserId(), id);
    }

    @ApiOperation("商品详情")
    @GetMapping("/{id}")
    public Result<GoodsVO> getGoodsDetail(@PathVariable Long id) {
        return goodsService.getGoodsDetail(id, SecurityUtil.getCurrentUserId());
    }

    @ApiOperation("商品列表")
    @GetMapping
    public Result<PageResult<GoodsVO>> listGoods(GoodsQueryDTO dto) {
        return goodsService.listGoods(dto);
    }

    @ApiOperation("热门商品")
    @GetMapping("/hot")
    public Result<PageResult<GoodsVO>> hotGoods() {
        return goodsService.hotGoods();
    }

    @ApiOperation("推荐商品")
    @GetMapping("/recommend")
    public Result<PageResult<GoodsVO>> recommendGoods() {
        return goodsService.recommendGoods();
    }

    @ApiOperation("提交审核")
    @PutMapping("/{id}/submit")
    public Result<Void> submitAudit(@PathVariable Long id) {
        return goodsService.submitAudit(SecurityUtil.requireCurrentUserId(), id);
    }

    @ApiOperation("上架")
    @PutMapping("/{id}/online")
    public Result<Void> onlineGoods(@PathVariable Long id) {
        return goodsService.onlineGoods(SecurityUtil.requireCurrentUserId(), id);
    }

    @ApiOperation("下架")
    @PutMapping("/{id}/offline")
    public Result<Void> offlineGoods(@PathVariable Long id) {
        return goodsService.offlineGoods(SecurityUtil.requireCurrentUserId(), id);
    }

    @ApiOperation("收藏")
    @PostMapping("/{id}/favorite")
    public Result<Void> favoriteGoods(@PathVariable Long id) {
        return goodsService.favoriteGoods(SecurityUtil.requireCurrentUserId(), id);
    }

    @ApiOperation("取消收藏")
    @DeleteMapping("/{id}/favorite")
    public Result<Void> unfavoriteGoods(@PathVariable Long id) {
        return goodsService.unfavoriteGoods(SecurityUtil.requireCurrentUserId(), id);
    }

    @ApiOperation("收藏列表")
    @GetMapping("/favorites")
    public Result<PageResult<GoodsVO>> listFavorites(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {
        return goodsService.listFavoriteGoods(SecurityUtil.requireCurrentUserId(), pageNum, pageSize, status);
    }

    @ApiOperation("我的商品")
    @GetMapping("/mine")
    public Result<PageResult<GoodsVO>> listMyGoods(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {
        GoodsQueryDTO dto = new GoodsQueryDTO();
        dto.setPageNum(pageNum);
        dto.setPageSize(pageSize);
        dto.setUserId(SecurityUtil.requireCurrentUserId());
        dto.setStatus(status);
        return goodsService.listGoods(dto);
    }
}
