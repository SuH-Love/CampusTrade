package com.campustrade.controller;

import com.campustrade.entity.GoodsCategory;
import com.campustrade.mapper.GoodsCategoryMapper;
import com.campustrade.service.GoodsCategoryService;
import com.campustrade.common.Result;
import com.campustrade.vo.GoodsCategoryVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "商品分类接口")
@RestController
@RequestMapping("/api/goods-category")
public class GoodsCategoryController {

    @Autowired
    private GoodsCategoryService categoryService;

    @Autowired
    private GoodsCategoryMapper categoryMapper;

    @ApiOperation("分类列表")
    @GetMapping
    public Result<List<GoodsCategoryVO>> listAll() {
        return Result.success(categoryService.listAllVO());
    }

    @ApiOperation("创建分类")
    @PostMapping
    @PreAuthorize("hasAuthority('goods:audit')")
    public Result<Void> createCategory(@RequestBody GoodsCategory category) {
        category.setId(null);
        categoryMapper.insert(category);
        return Result.success();
    }

    @ApiOperation("修改分类")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('goods:audit')")
    public Result<Void> updateCategory(@PathVariable Long id, @RequestBody GoodsCategory category) {
        category.setId(id);
        categoryMapper.updateById(category);
        return Result.success();
    }

    @ApiOperation("删除分类")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('goods:audit')")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        GoodsCategory category = categoryMapper.selectById(id);
        if (category == null) return Result.error(404, "分类不存在");
        category.setDeleted(1);
        categoryMapper.updateById(category);
        return Result.success();
    }
}
