package com.campustrade.mapper;

import com.campustrade.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserRoleMapper {

    int insert(UserRole userRole);

    int deleteByUserId(@Param("userId") Long userId);
}