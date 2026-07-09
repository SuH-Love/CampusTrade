package com.campustrade.mapper;

import com.campustrade.entity.Banner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BannerMapper {

    Banner selectById(@Param("id") Long id);

    List<Banner> selectActive();

    List<Banner> selectAll(@Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    Long selectCount();

    int insert(Banner banner);

    int updateById(Banner banner);

    int logicDeleteById(@Param("id") Long id);
}