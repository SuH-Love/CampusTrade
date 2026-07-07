package com.campustrade.controller;

import com.campustrade.service.GoodsCategoryService;
import com.campustrade.common.Result;
import com.campustrade.vo.GoodsCategoryVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "商品分类接口")
@RestController
@RequestMapping("/api/goods-category")
public class GoodsCategoryController {

    @Autowired
    private GoodsCategoryService categoryService;

    @ApiOperation("分类列表")
    @GetMapping
    public Result<List<GoodsCategoryVO>> listAll() {
        return Result.success(categoryService.listAllVO());
    }
}
