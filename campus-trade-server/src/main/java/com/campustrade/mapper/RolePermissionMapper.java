package com.campustrade.mapper;

import com.campustrade.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RolePermissionMapper {

    int insert(RolePermission rolePermission);

    int deleteByRoleId(Long roleId);

    List<Long> selectPermissionIdsByRoleId(Long roleId);

    RolePermission selectByRoleIdAndPermissionId(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);
}