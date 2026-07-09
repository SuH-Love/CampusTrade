package com.campustrade.vo;

import lombok.Data;

import java.util.List;

@Data
public class AdminInfoVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private List<String> roles;
    private List<String> permissions;
}