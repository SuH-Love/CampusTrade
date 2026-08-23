package com.campustrade.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class SendCodeDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    private String email;
}
