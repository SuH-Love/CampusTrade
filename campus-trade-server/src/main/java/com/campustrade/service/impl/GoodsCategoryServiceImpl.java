package com.campustrade.service.impl;

import com.campustrade.entity.GoodsCategory;
import com.campustrade.mapper.GoodsCategoryMapper;
import com.campustrade.service.GoodsCategoryService;
import com.campustrade.vo.GoodsCategoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoodsCategoryServiceImpl implements GoodsCategoryService {

    @Autowired
    private GoodsCategoryMapper categoryMapper;

    @Override
    public List<GoodsCategory> listAll() {
        return categoryMapper.selectAll();
    }

    @Override
    public List<GoodsCategoryVO> listAllVO() {
        return categoryMapper.selectAll().stream().map(this::toVO).collect(Collectors.toList());
    }

    private GoodsCategoryVO toVO(GoodsCategory category) {
        GoodsCategoryVO vo = new GoodsCategoryVO();
        vo.setId(category.getId());
        vo.setCategoryName(category.getCategoryName());
        vo.setParentId(category.getParentId());
        vo.setSortOrder(category.getSortOrder());
        vo.setIcon(category.getIcon());
        vo.setStatus(category.getStatus());
        vo.setCreateTime(category.getCreateTime() != null ? category.getCreateTime().toString() : null);
        return vo;
    }
}
