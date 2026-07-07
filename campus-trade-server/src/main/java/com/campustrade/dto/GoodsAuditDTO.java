package com.campustrade.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class GoodsAuditDTO {

    @NotBlank(message = "审核状态不能为空")
    private String status;

    private String rejectReason;
}