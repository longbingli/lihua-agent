package com.bingli.lihuaAgent.common;

/**
 * 自定义错误码
 *
 */
public enum ErrorCode {

    SUCCESS(200, "ok"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败"),

    AI_MODEL_NOT_CONFIGURED(51000, "AI 模型未配置"),
    AI_MODEL_UNAVAILABLE(51001, "AI 模型暂不可用"),
    AI_REQUEST_TIMEOUT(51002, "AI 请求超时"),
    AI_RESPONSE_EMPTY(51003, "AI 响应为空"),
    AI_RATE_LIMITED(51004, "AI 请求过于频繁"),
    AI_CONTEXT_TOO_LONG(51005, "AI 上下文过长"),
    AI_TOKEN_QUOTA_EXCEEDED(51006, "AI 配额不足"),
    AI_STREAM_INTERRUPTED(51007, "AI 流式响应中断"),
    AI_STREAM_SEND_FAILED(51008, "AI 流式消息发送失败"),
    AI_FALLBACK_EXHAUSTED(51009, "AI 备用模型均不可用"),
    AI_SERVICE_CONFIG_INVALID(51010, "AI 服务配置无效"),
    AI_CONTENT_BLOCKED(51011, "AI 响应内容受限");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
