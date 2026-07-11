package com.campustrade.controller;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.dto.GoodsCreateDTO;
import com.campustrade.dto.GoodsQueryDTO;
import com.campustrade.dto.GoodsUpdateDTO;
import com.campustrade.entity.Goods;
import com.campustrade.entity.GoodsCategory;
import com.campustrade.mapper.GoodsCategoryMapper;
import com.campustrade.mapper.GoodsMapper;
import com.campustrade.service.GoodsService;
import com.campustrade.util.SecurityUtil;
import com.campustrade.vo.GoodsVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Api(tags = "商品接口")
@RestController
@RequestMapping("/api/goods")
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private GoodsCategoryMapper categoryMapper;

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

    @ApiOperation("热门搜索词")
    @GetMapping("/hot-keywords")
    public Result<List<String>> hotKeywords() {
        Set<String> keywords = new LinkedHashSet<>();

        List<GoodsCategory> cats = categoryMapper.selectAll();
        for (GoodsCategory cat : cats) {
            if (cat.getCategoryName() != null && cat.getCategoryName().length() >= 2) {
                keywords.add(cat.getCategoryName());
            }
        }

        List<Goods> hotGoods = goodsMapper.selectHotGoods(50);
        Map<String, Integer> freq = new HashMap<>();
        Set<String> stopWords = new HashSet<>(Arrays.asList(
                "的", "了", "是", "在", "和", "与", "及", "等", "可", "用", "有", "无",
                "不", "很", "都", "还", "就", "要", "会", "对", "中", "为", "到", "把",
                "被", "让", "给", "从", "上", "下", "出", "入", "个", "只", "这", "那",
                "我", "你", "他", "她", "它", "一", "二", "三", "四", "五", "六",
                "七", "八", "九", "十", "百", "千", "万", "元", "块", "毛", "分",
                "新", "旧", "好", "坏", "大", "小", "多", "少", "高", "低", "长", "短",
                "出", "售", "转", "卖", "买", "急", "优", "超", "最", "特", "自", "非"
        ));
        for (Goods g : hotGoods) {
            if (g.getTitle() == null) continue;
            String title = g.getTitle().replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", " ").trim();
            for (int len = 2; len <= 4; len++) {
                String[] parts = title.split("\\s+");
                for (String part : parts) {
                    if (part.length() < len) continue;
                    for (int i = 0; i <= part.length() - len; i++) {
                        String word = part.substring(i, i + len);
                        if (stopWords.contains(word)) continue;
                        boolean allStop = true;
                        for (char c : word.toCharArray()) {
                            if (!stopWords.contains(String.valueOf(c))) { allStop = false; break; }
                        }
                        if (allStop) continue;
                        freq.merge(word, 1, Integer::sum);
                    }
                }
            }
        }
        freq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .map(Map.Entry::getKey)
                .forEach(keywords::add);

        return Result.success(keywords.stream().limit(10).collect(Collectors.toList()));
    }
}
