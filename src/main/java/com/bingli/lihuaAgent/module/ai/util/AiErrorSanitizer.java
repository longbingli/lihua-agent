package com.bingli.lihuaAgent.module.ai.util;

import org.springframework.util.StringUtils;

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
}
