package com.campustrade.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class UserUpdateDTO {

    @Size(max = 50, message = "昵称最长50")
    private String nickname;

    private String phone;

    private String email;

    @Size(max = 50, message = "真实姓名最长50")
    private String realName;

    @Size(max = 50, message = "学号最长50")
    private String studentId;
}