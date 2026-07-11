package com.campustrade.mapper;

import com.campustrade.entity.UserBlacklist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserBlacklistMapper {
    UserBlacklist selectByUserAndBlocked(@Param("userId") Long userId, @Param("blockedId") Long blockedId);
    List<UserBlacklist> selectByUserId(@Param("userId") Long userId);
    int insert(UserBlacklist blacklist);
    int deleteById(@Param("id") Long id);
}