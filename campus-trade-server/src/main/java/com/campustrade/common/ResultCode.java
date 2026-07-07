package com.campustrade.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "参数错误"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "不存在"),
    TOO_MANY_REQUESTS(429, "频率过高"),
    INTERNAL_ERROR(500, "系统异常"),
    SYSTEM_ERROR(500, "系统异常"),

    USERNAME_EXISTS(1001, "用户名已存在"),
    PHONE_EXISTS(1002, "手机号已注册"),
    EMAIL_EXISTS(1003, "邮箱已注册"),
    LOGIN_FAIL(1004, "用户名或密码错误"),
    TOKEN_EXPIRED(1005, "Token已过期"),
    TOKEN_INVALID(1006, "Token无效"),
    ACCOUNT_DISABLED(1007, "账号已被禁用"),
    OLD_PASSWORD_ERROR(1008, "原密码错误"),

    GOODS_NOT_FOUND(2001, "商品不存在"),
    GOODS_NOT_OWNER(2002, "非商品所有者"),
    GOODS_STATUS_ERROR(2003, "商品状态异常"),
    GOODS_ALREADY_FAVORITED(2004, "已收藏该商品"),

    ORDER_NOT_FOUND(3001, "订单不存在"),
    ORDER_STATUS_ERROR(3002, "订单状态异常"),
    ORDER_NOT_OWNER(3003, "非订单所有者"),
    ORDER_REPEAT_SUBMIT(3004, "请勿重复提交"),

    REPORT_NOT_FOUND(4001, "举报不存在"),
    REPORT_ALREADY_EXISTS(4002, "已举报过"),

    CHAT_NOT_FOUND(5001, "消息不存在"),

    RATE_LIMIT_EXCEEDED(6001, "请求过于频繁"),
    REPEAT_SUBMIT(6002, "请勿重复提交"),
    DATA_VERSION_ERROR(6003, "数据已被修改，请刷新后重试");

    private final int code;
    private final String message;
}