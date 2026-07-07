package com.campustrade.mapper;

import com.campustrade.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface UserMapper {

    User selectById(@Param("id") Long id);

    List<User> selectByIds(@Param("ids") Collection<Long> ids);

    User selectByUsername(@Param("username") String username);

    User selectByPhone(@Param("phone") String phone);

    User selectByEmail(@Param("email") String email);

    List<User> selectList(@Param("username") String username, @Param("status") Integer status,
                          @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    Long selectCount(@Param("username") String username, @Param("status") Integer status);

    int insert(User user);

    int updateById(User user);

    int logicDeleteById(@Param("id") Long id);
}