package com.bingli.lihuaAgent.common;

/**
 * 自定义错误码
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
    AI_CONTENT_BLOCKED(51011, "AI 响应内容受限"),
    AI_ATTACHMENT_INVALID(51012, "AI 附件无效"),
    AI_ATTACHMENT_EXPIRED(51013, "AI 附件已过期"),
    AI_ATTACHMENT_TOO_LARGE(51014, "AI 附件过大"),
    AI_ATTACHMENT_PARSE_FAILED(51015, "AI 附件解析失败"),
    AI_ATTACHMENT_UNSUPPORTED_TYPE(51016, "AI 附件类型不支持"),

    RAG_KB_NOT_FOUND(52000, "知识库不存在"),
    RAG_DOCUMENT_NOT_FOUND(52001, "文档不存在"),
    RAG_DOCUMENT_STATUS_INVALID(52002, "文档状态不允许当前操作"),
    RAG_FILE_UNSUPPORTED(52003, "RAG 文档类型不支持"),
    RAG_PARSE_FAILED(52004, "RAG 文档解析失败"),
    RAG_CHUNK_FAILED(52005, "RAG 文档分块失败"),
    RAG_EMBEDDING_FAILED(52006, "RAG 向量化失败"),
    RAG_VECTOR_STORE_FAILED(52007, "RAG 向量存储失败"),
    RAG_RETRIEVAL_FAILED(52008, "RAG 检索失败"),
    RAG_DOCUMENT_NOT_READY(52009, "RAG 文档尚未处理完成");

    private final int code;

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
