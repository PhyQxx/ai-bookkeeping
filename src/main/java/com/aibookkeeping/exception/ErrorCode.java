package com.aibookkeeping.exception;

import com.aibookkeeping.service.auth.AuthServiceImpl;
import com.aibookkeeping.service.bill.BillServiceImpl;
import com.aibookkeeping.service.category.CategoryServiceImpl;
import lombok.Getter;

/**
 * 业务异常枚举，统一错误码管理
 */
@Getter
public enum ErrorCode {

    // 通用 1xxx
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 认证 10xx
    USER_ALREADY_EXISTS(1001, "用户名已存在"),
    USER_NOT_FOUND(1002, "用户不存在"),
    INVALID_CREDENTIALS(1003, "用户名或密码错误"),
    TOKEN_EXPIRED(1004, "Token已过期"),
    TOKEN_INVALID(1005, "Token无效"),

    // 账单 20xx
    BILL_NOT_FOUND(2001, "账单不存在"),
    BILL_NO_PERMISSION(2002, "无权限操作此账单"),
    AI_PARSE_FAILED(2003, "AI解析失败，请尝试更详细的描述或手动输入"),
    AI_PARSE_AMOUNT_INVALID(2004, "AI解析金额无效"),

    // 分类 30xx
    CATEGORY_NOT_FOUND(3001, "分类不存在"),
    CATEGORY_SYSTEM_CANNOT_MODIFY(3002, "系统预设分类不可修改"),
    CATEGORY_SYSTEM_CANNOT_DELETE(3003, "系统预设分类不可删除"),
    CATEGORY_NO_PERMISSION(3004, "无权限操作此分类"),
    CATEGORY_ALREADY_EXISTS(3005, "分类已存在"),

    // AI 40xx
    AI_SERVICE_UNAVAILABLE(4001, "AI服务不可用"),
    AI_RATE_LIMITED(4002, "AI调用频率超限，请稍后再试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
