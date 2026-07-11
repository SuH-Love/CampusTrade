package com.campustrade.mapper;

import com.campustrade.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnnouncementMapper {
    Announcement selectById(@Param("id") Long id);
    List<Announcement> selectAll(@Param("offset") Integer offset, @Param("pageSize") Integer pageSize);
    Long selectCount();
    List<Announcement> selectActive();
    int insert(Announcement announcement);
    int updateById(Announcement announcement);
    int logicDeleteById(@Param("id") Long id);
}