package com.campustrade.mapper;

import com.campustrade.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PermissionMapper {

    Permission selectById(@Param("id") Long id);

    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    List<Permission> selectByUserId(@Param("userId") Long userId);

    List<Permission> selectAll();

    List<Permission> selectByRoleId(@Param("roleId") Long roleId);

    int insert(Permission permission);

    int updateById(Permission permission);
}