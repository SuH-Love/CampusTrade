package com.campustrade.mapper;

import com.campustrade.entity.GoodsCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface GoodsCategoryMapper {

    GoodsCategory selectById(@Param("id") Long id);

    List<GoodsCategory> selectByIds(@Param("ids") Collection<Long> ids);

    List<GoodsCategory> selectAll();

    int insert(GoodsCategory category);

    Long selectCountAll();

    int updateById(GoodsCategory category);
}