package com.kite.common.response;

import lombok.Getter;

/**
 * 统一响应状态码
 */
@Getter
public enum ResultCode {
    
    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    
    // 客户端错误 4xx
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    
    // 业务错误 5xx
    BUSINESS_ERROR(5000, "业务异常"),
    SYSTEM_ERROR(5001, "系统异常"),
    
    // 认证相关 6xx
    TOKEN_INVALID(6001, "Token无效"),
    TOKEN_EXPIRED(6002, "Token已过期"),
    TOKEN_BLACKLIST(6003, "Token已失效"),
    LOGIN_FAILED(6004, "登录失败"),
    USER_NOT_FOUND(6005, "用户不存在"),
    USER_DISABLED(6006, "用户已禁用"),
    PASSWORD_ERROR(6007, "密码错误"),
    
    // 权限相关 7xx
    NO_PERMISSION(7001, "无权限访问"),
    NO_ROLE(7002, "无角色权限"),
    
    // 限流相关 8xx
    RATE_LIMIT(8001, "请求过于频繁，请稍后再试"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试");
    
    private final Integer code;
    private final String message;
    
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
