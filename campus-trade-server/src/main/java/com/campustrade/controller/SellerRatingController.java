package com.campustrade.controller;

import com.campustrade.common.Result;
import com.campustrade.entity.SellerRating;
import com.campustrade.mapper.SellerRatingMapper;
import com.campustrade.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "商家评分接口")
@RestController
@RequestMapping("/api/rating")
public class SellerRatingController {
    @Autowired
    private SellerRatingMapper sellerRatingMapper;

    @ApiOperation("获取商家平均评分")
    @GetMapping("/average/{sellerId}")
    public Result<Double> getAverageRating(@PathVariable Long sellerId) {
        Double avg = sellerRatingMapper.selectAvgRatingBySellerId(sellerId);
        return Result.success(avg != null ? Math.round(avg * 10) / 10.0 : 0.0);
    }
}