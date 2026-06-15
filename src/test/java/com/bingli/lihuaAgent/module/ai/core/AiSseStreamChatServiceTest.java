package com.bingli.lihuaAgent.module.ai.core;

import com.bingli.lihuaAgent.exception.BusinessException;
import com.bingli.lihuaAgent.module.ai.config.AiModuleProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiSseStreamChatServiceTest {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final AiAttachmentResolver attachmentResolver = new AiAttachmentResolver(null, new AiModuleProperties());

    @Test
    void shouldRejectBlankMessage() {
        AiSseStreamChatService service = new AiSseStreamChatService(List.of(), "你是企业助手", executorService, attachmentResolver);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.streamChat(" ", null, null, null, null, 1L));

        assertEquals("消息内容和附件不能同时为空", exception.getMessage());
    }

    @Test
    void shouldRejectWhenNoStreamingModelConfigured() {
        AiSseStreamChatService service = new AiSseStreamChatService(List.of(), "你是企业助手", executorService, attachmentResolver);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.streamChat("你好", null, null, null, null, 1L));

        assertEquals("未配置可用的流式 AI 模型", exception.getMessage());
    }
}
