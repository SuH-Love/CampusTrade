package com.campustrade.controller;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.entity.UserFollow;
import com.campustrade.mapper.UserFollowMapper;
import com.campustrade.mapper.UserMapper;
import com.campustrade.mapper.GoodsMapper;
import com.campustrade.util.SecurityUtil;
import com.campustrade.vo.UserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import com.campustrade.mapper.SellerRatingMapper;

@Api(tags = "关注接口")
@RestController
@RequestMapping("/api/follow")
public class UserFollowController {
    @Autowired
    private UserFollowMapper userFollowMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private GoodsMapper goodsMapper;
    @Autowired
    private SellerRatingMapper sellerRatingMapper;

    @ApiOperation("关注/取关用户")
    @PostMapping("/{userId}")
    public Result<Void> toggleFollow(@PathVariable Long userId) {
        Long myId = SecurityUtil.requireCurrentUserId();
        if (myId.equals(userId)) return Result.error(400, "不能关注自己");
        UserFollow existing = userFollowMapper.selectByFollowerAndFollowing(myId, userId);
        if (existing != null) {
            userFollowMapper.deleteByFollowerAndFollowing(myId, userId);
        } else {
            UserFollow follow = new UserFollow();
            follow.setFollowerId(myId);
            follow.setFollowingId(userId);
            try {
                userFollowMapper.insert(follow);
            } catch (Exception e) {
                userFollowMapper.restoreByFollowerAndFollowing(myId, userId);
            }
        }
        return Result.success();
    }

    @ApiOperation("是否已关注")
    @GetMapping("/is-following/{userId}")
    public Result<Boolean> isFollowing(@PathVariable Long userId) {
        Long myId = SecurityUtil.requireCurrentUserId();
        UserFollow existing = userFollowMapper.selectByFollowerAndFollowing(myId, userId);
        return Result.success(existing != null);
    }

    @ApiOperation("我的关注列表")
    @GetMapping("/following")
    public Result<PageResult<UserVO>> listFollowing(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long myId = SecurityUtil.requireCurrentUserId();
        int offset = (pageNum - 1) * pageSize;
        List<Long> ids = userFollowMapper.selectFollowingIds(myId, offset, pageSize);
        Long total = userFollowMapper.selectFollowingCount(myId);
        if (ids.isEmpty()) return Result.success(new PageResult<>(List.of(), 0L));
        List<UserVO> vos = userMapper.selectByIds(ids).stream().map(u -> {
            UserVO vo = new UserVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setNickname(u.getNickname());
            vo.setAvatar(u.getAvatar());
            vo.setFollowersCount(userFollowMapper.selectFollowerCount(u.getId()));
            vo.setFollowingCount(userFollowMapper.selectFollowingCount(u.getId()));
            vo.setGoodsCount(goodsMapper.selectCount(null, null, null, null, "ONLINE", u.getId()));
            vo.setSoldCount(goodsMapper.selectCountByStatusAndUserId("SOLD", u.getId()));
            Double avg = sellerRatingMapper.selectAvgRatingBySellerId(u.getId());
            vo.setAvgRating(avg != null ? avg : 0.0);
            return vo;
        }).collect(Collectors.toList());
        return Result.success(new PageResult<>(vos, total));
    }

    @ApiOperation("我的粉丝列表")
    @GetMapping("/followers/{userId}")
    public Result<PageResult<UserVO>> listFollowers(@PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Long> ids = userFollowMapper.selectFollowerIds(userId, offset, pageSize);
        Long total = userFollowMapper.selectFollowerCount(userId);
        if (ids.isEmpty()) return Result.success(new PageResult<>(List.of(), 0L));
        List<UserVO> vos = userMapper.selectByIds(ids).stream().map(u -> {
            UserVO vo = new UserVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setNickname(u.getNickname());
            vo.setAvatar(u.getAvatar());
            return vo;
        }).collect(Collectors.toList());
        return Result.success(new PageResult<>(vos, total));
    }

    @ApiOperation("关注数/粉丝数")
    @GetMapping("/counts/{userId}")
    public Result<java.util.Map<String, Long>> getFollowCounts(@PathVariable Long userId) {
        Long followingCount = userFollowMapper.selectFollowingCount(userId);
        Long followerCount = userFollowMapper.selectFollowerCount(userId);
        return Result.success(java.util.Map.of("following", followingCount, "followers", followerCount));
    }
}