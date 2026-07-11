package com.campustrade.controller;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.entity.SellerRating;
import com.campustrade.entity.User;
import com.campustrade.mapper.SellerRatingMapper;
import com.campustrade.mapper.UserMapper;
import com.campustrade.vo.SellerRatingVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "商家评分接口")
@RestController
@RequestMapping("/api/rating")
public class SellerRatingController {
    @Autowired
    private SellerRatingMapper sellerRatingMapper;
    @Autowired
    private UserMapper userMapper;

    @ApiOperation("获取商家平均评分")
    @GetMapping("/average/{sellerId}")
    public Result<Double> getAverageRating(@PathVariable Long sellerId) {
        Double avg = sellerRatingMapper.selectAvgRatingBySellerId(sellerId);
        return Result.success(avg != null ? Math.round(avg * 10) / 10.0 : 0.0);
    }

    @ApiOperation("获取商家评价列表")
    @GetMapping("/list/{sellerId}")
    public Result<PageResult<SellerRatingVO>> getRatingList(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<SellerRating> ratings = sellerRatingMapper.selectBySellerId(sellerId, offset, pageSize);
        Long total = sellerRatingMapper.selectCountBySellerId(sellerId);
        List<SellerRatingVO> voList = new ArrayList<>();
        for (SellerRating r : ratings) {
            SellerRatingVO vo = new SellerRatingVO();
            vo.setId(r.getId());
            vo.setOrderId(r.getOrderId());
            vo.setBuyerId(r.getBuyerId());
            vo.setSellerId(r.getSellerId());
            vo.setRating(r.getRating());
            vo.setComment(r.getComment());
            vo.setCreateTime(r.getCreateTime());
            User buyer = userMapper.selectById(r.getBuyerId());
            if (buyer != null) {
                vo.setBuyerName(buyer.getNickname() != null ? buyer.getNickname() : buyer.getUsername());
                vo.setBuyerAvatar(buyer.getAvatar());
            }
            voList.add(vo);
        }
        return Result.success(new PageResult<>(voList, total));
    }

    @ApiOperation("获取商家评分分布")
    @GetMapping("/distribution/{sellerId}")
    public Result<Map<String, Object>> getRatingDistribution(@PathVariable Long sellerId) {
        List<Map<String, Object>> distribution = sellerRatingMapper.selectRatingDistributionBySellerId(sellerId);
        Long totalCount = sellerRatingMapper.selectCountBySellerId(sellerId);
        Double avgRating = sellerRatingMapper.selectAvgRatingBySellerId(sellerId);
        Map<String, Object> result = new HashMap<>();
        Map<Integer, Long> distMap = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            distMap.put(i, 0L);
        }
        for (Map<String, Object> item : distribution) {
            Integer rating = ((Number) item.get("rating")).intValue();
            Long count = ((Number) item.get("count")).longValue();
            distMap.put(rating, count);
        }
        result.put("distribution", distMap);
        result.put("totalCount", totalCount);
        result.put("avgRating", avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0);
        return Result.success(result);
    }
}
