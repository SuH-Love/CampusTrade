package com.campustrade.mapper;

import com.campustrade.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleMapper {

    Role selectById(@Param("id") Long id);

    List<Role> selectByUserId(@Param("userId") Long userId);

    List<Role> selectAll();

    int insert(Role role);

    int updateById(Role role);
}