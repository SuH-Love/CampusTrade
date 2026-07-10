package com.campustrade.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private String avatar;
    private String realName;
    private String studentId;
    private Integer realVerified;
    private Integer status;
    private LocalDateTime createTime;
    private Long followingCount;
    private Long followersCount;
    private Long goodsCount;
}