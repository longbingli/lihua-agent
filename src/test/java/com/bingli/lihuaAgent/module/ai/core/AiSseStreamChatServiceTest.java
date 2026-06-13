package com.bingli.lihuaAgent.module.ai.core;

import com.bingli.lihuaAgent.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiSseStreamChatServiceTest {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Test
    void shouldRejectBlankMessage() {
        AiSseStreamChatService service = new AiSseStreamChatService(List.of(), "你是企业助手", executorService);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.streamChat(" "));

        assertEquals("消息内容不能为空", exception.getMessage());
    }

    @Test
    void shouldRejectWhenNoStreamingModelConfigured() {
        AiSseStreamChatService service = new AiSseStreamChatService(List.of(), "你是企业助手", executorService);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.streamChat("你好"));

        assertEquals("未配置可用的流式 AI 模型", exception.getMessage());
    }
}
