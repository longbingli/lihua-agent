package com.bingli.lihuaAgent.module.ai.util;

import com.bingli.lihuaAgent.common.ErrorCode;
import com.bingli.lihuaAgent.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.util.Locale;

public final class AiErrorSanitizer {

    private AiErrorSanitizer() {
    }

    public static String sanitize(Throwable throwable) {
        String message = throwable == null ? null : throwable.getMessage();
        if (!StringUtils.hasText(message)) {
            return "无异常消息";
        }
        return message.replaceAll("(?i)(api[-_ ]?key|authorization|bearer)\\s*[:=]\\s*[^,\\s]+", "$1=******");
    }

    public static ErrorCode resolveErrorCode(Throwable throwable, ErrorCode defaultCode) {
        if (throwable instanceof BusinessException businessException) {
            return findByCode(businessException.getCode(), defaultCode);
        }
        String message = sanitize(throwable).toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(message) || "无异常消息".equals(message)) {
            return defaultCode;
        }
        if (containsAny(message, "timeout", "timed out", "超时")) {
            return ErrorCode.AI_REQUEST_TIMEOUT;
        }
        if (containsAny(message, "rate limit", "too many requests", "429", "请求过于频繁", "限流")) {
            return ErrorCode.AI_RATE_LIMITED;
        }
        if (containsAny(message, "context length", "context window", "maximum context", "prompt is too long", "上下文过长", "token limit")) {
            return ErrorCode.AI_CONTEXT_TOO_LONG;
        }
        if (containsAny(message, "insufficient_quota", "quota", "余额不足", "配额不足", "credit")) {
            return ErrorCode.AI_TOKEN_QUOTA_EXCEEDED;
        }
        if (containsAny(message, "content filter", "safety", "policy", "blocked", "内容受限", "审核")) {
            return ErrorCode.AI_CONTENT_BLOCKED;
        }
        if (containsAny(message, "api key", "baseurl", "modelname", "alias", "unauthorized", "401", "invalid_request_error", "配置", "不能为空")) {
            return ErrorCode.AI_SERVICE_CONFIG_INVALID;
        }
        if (containsAny(message, "response empty", "empty response", "响应为空")) {
            return ErrorCode.AI_RESPONSE_EMPTY;
        }
        if (containsAny(message, "sse", "stream", "流式", "broken pipe", "connection reset")) {
            return ErrorCode.AI_STREAM_INTERRUPTED;
        }
        if (containsAny(message, "model unavailable", "service unavailable", "502", "503", "504", "connection refused", "connect timed out")) {
            return ErrorCode.AI_MODEL_UNAVAILABLE;
        }
        return defaultCode;
    }

    public static BusinessException toBusinessException(Throwable throwable, ErrorCode defaultCode, String defaultMessage) {
        if (throwable instanceof BusinessException businessException) {
            return businessException;
        }
        ErrorCode errorCode = resolveErrorCode(throwable, defaultCode);
        String message = sanitize(throwable);
        if (!StringUtils.hasText(message) || "无异常消息".equals(message)) {
            message = defaultMessage;
        }
        return new BusinessException(errorCode, message);
    }

    private static ErrorCode findByCode(int code, ErrorCode defaultCode) {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.getCode() == code) {
                return errorCode;
            }
        }
        return defaultCode;
    }

    private static boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
