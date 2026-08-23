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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
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

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String SEARCH_KEYWORDS_KEY = "search:keywords";
    private static final String VIEWED_TITLES_KEY = "search:viewed_titles";

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
        Result<GoodsVO> result = goodsService.getGoodsDetail(id, SecurityUtil.getCurrentUserId());
        if (result.getData() != null && result.getData().getTitle() != null) {
            try {
                String title = result.getData().getTitle().trim();
                if (title.length() >= 2 && title.length() <= 20) {
                    stringRedisTemplate.opsForZSet().incrementScore(VIEWED_TITLES_KEY, title, 1);
                    stringRedisTemplate.expire(VIEWED_TITLES_KEY, 7, TimeUnit.DAYS);
                }
            } catch (Exception ignored) {}
        }
        return result;
    }

    @ApiOperation("商品列表")
    @GetMapping
    public Result<PageResult<GoodsVO>> listGoods(GoodsQueryDTO dto) {
        if (dto.getKeyword() != null && !dto.getKeyword().trim().isEmpty()) {
            try {
                String kw = dto.getKeyword().trim();
                if (kw.length() >= 1 && kw.length() <= 20) {
                    stringRedisTemplate.opsForZSet().incrementScore(SEARCH_KEYWORDS_KEY, kw, 1);
                    stringRedisTemplate.expire(SEARCH_KEYWORDS_KEY, 7, TimeUnit.DAYS);
                }
            } catch (Exception ignored) {}
        }
        return goodsService.listGoods(dto);
    }

    @ApiOperation("搜索联想")
    @GetMapping("/suggest")
    public Result<List<String>> suggest(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return Result.success(List.of());
        String kw = keyword.trim();
        Set<String> suggestions = new LinkedHashSet<>();

        List<GoodsVO> goods = goodsMapper.selectHotGoodsVO(100);
        for (GoodsVO g : goods) {
            if (g.getTitle() != null && g.getTitle().toLowerCase().contains(kw.toLowerCase())) {
                suggestions.add(g.getTitle());
            }
            if (g.getUsername() != null && g.getUsername().toLowerCase().contains(kw.toLowerCase())) {
                suggestions.add(g.getUsername());
            }
            if (suggestions.size() >= 8) break;
        }

        List<GoodsCategory> cats = categoryMapper.selectAll();
        for (GoodsCategory cat : cats) {
            if (cat.getCategoryName() != null && cat.getCategoryName().toLowerCase().contains(kw.toLowerCase())) {
                suggestions.add(cat.getCategoryName());
            }
        }

        try {
            Set<String> topSearched = stringRedisTemplate.opsForZSet().reverseRange(SEARCH_KEYWORDS_KEY, 0, 49);
            if (topSearched != null) {
                for (String s : topSearched) {
                    if (s.toLowerCase().contains(kw.toLowerCase())) suggestions.add(s);
                }
            }
        } catch (Exception ignored) {}

        return Result.success(suggestions.stream().limit(10).collect(Collectors.toList()));
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
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return goodsService.listFavoriteGoods(SecurityUtil.requireCurrentUserId(), pageNum, pageSize, keyword, status);
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
    public Result<List<Map<String, Object>>> hotKeywords() {
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // 1. 真实用户搜索词（带分数，区分热门/新趋势）
        try {
            var topWithScores = stringRedisTemplate.opsForZSet()
                    .reverseRangeWithScores(SEARCH_KEYWORDS_KEY, 0, 14);
            if (topWithScores != null) {
                int rank = 0;
                for (var tuple : topWithScores) {
                    String kw = tuple.getValue();
                    if (kw == null || seen.contains(kw)) continue;
                    seen.add(kw);
                    Map<String, Object> item = new HashMap<>();
                    item.put("keyword", kw);
                    item.put("type", rank < 3 ? "hot" : "new");
                    result.add(item);
                    rank++;
                }
            }
        } catch (Exception ignored) {}

        // 2. 高频浏览商品标题提取关键词
        try {
            Set<String> topViewed = stringRedisTemplate.opsForZSet()
                    .reverseRange(VIEWED_TITLES_KEY, 0, 19);
            if (topViewed != null) {
                for (String title : topViewed) {
                    if (result.size() >= 8) break;
                    String clean = title.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", " ").trim();
                    for (String part : clean.split("\\s+")) {
                        if (part.length() >= 2 && part.length() <= 6 && !seen.contains(part) && result.size() < 8) {
                            seen.add(part);
                            Map<String, Object> item = new HashMap<>();
                            item.put("keyword", part);
                            item.put("type", "");
                            result.add(item);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // 3. 分类名兜底
        if (result.size() < 5) {
            List<GoodsCategory> cats = categoryMapper.selectAll();
            for (GoodsCategory cat : cats) {
                if (result.size() >= 8) break;
                if (cat.getCategoryName() != null && cat.getCategoryName().length() >= 2 && !seen.contains(cat.getCategoryName())) {
                    seen.add(cat.getCategoryName());
                    Map<String, Object> item = new HashMap<>();
                    item.put("keyword", cat.getCategoryName());
                    item.put("type", "");
                    result.add(item);
                }
            }
        }

        return Result.success(result.stream().limit(10).collect(Collectors.toList()));
    }
}
