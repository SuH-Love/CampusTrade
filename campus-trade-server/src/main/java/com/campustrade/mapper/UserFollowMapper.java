package com.campustrade.mapper;

import com.campustrade.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserFollowMapper {
    UserFollow selectByFollowerAndFollowing(@Param("followerId") Long followerId, @Param("followingId") Long followingId);
    List<Long> selectFollowingIds(@Param("followerId") Long followerId, @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);
    Long selectFollowingCount(@Param("followerId") Long followerId);
    List<Long> selectFollowerIds(@Param("followingId") Long followingId, @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);
    Long selectFollowerCount(@Param("followingId") Long followingId);
    int insert(UserFollow follow);
    int deleteByFollowerAndFollowing(@Param("followerId") Long followerId, @Param("followingId") Long followingId);
}