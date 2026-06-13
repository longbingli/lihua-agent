package com.bingli.lihuaAgent.module.ai.core;

import com.bingli.lihuaAgent.exception.BusinessException;
import com.bingli.lihuaAgent.module.ai.api.model.AiChatResult;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiGatewayChatServiceTest {

    @Test
    void shouldFallbackWhenPrimaryModelFails() {
        AiGatewayChatService service = new AiGatewayChatService(List.of(
                new AiModelClient("primary", "primary-model", new FailingChatModel()),
                new AiModelClient("fallback", "fallback-model", new SuccessChatModel("备用模型响应"))
        ), "你是企业助手");

        AiChatResult response = service.chat("你好");

        assertEquals("备用模型响应", response.getContent());
        assertEquals("fallback", response.getModelAlias());
        assertEquals("fallback-model", response.getModelName());
        assertTrue(response.getFallbackUsed());
        assertEquals(3, response.getTotalTokens());
    }

    @Test
    void shouldThrowBusinessExceptionWhenAllModelsFail() {
        AiGatewayChatService service = new AiGatewayChatService(List.of(
                new AiModelClient("primary", "primary-model", new FailingChatModel()),
                new AiModelClient("fallback", "fallback-model", new FailingChatModel())
        ), "你是企业助手");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.chat("你好"));

        assertEquals("AI 模型服务暂不可用，请稍后重试", exception.getMessage());
    }

    @Test
    void shouldRejectBlankMessage() {
        AiGatewayChatService service = new AiGatewayChatService(List.of(
                new AiModelClient("primary", "primary-model", new SuccessChatModel("ok"))
        ), "你是企业助手");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.chat(" "));

        assertEquals("消息内容不能为空", exception.getMessage());
    }

    @Test
    void shouldUsePrimaryWhenPrimarySucceeds() {
        AtomicInteger calls = new AtomicInteger();
        AiGatewayChatService service = new AiGatewayChatService(List.of(
                new AiModelClient("primary", "primary-model", new CountingSuccessChatModel("primary-model", "主模型响应", calls)),
                new AiModelClient("fallback", "fallback-model", new SuccessChatModel("备用模型响应"))
        ), "你是企业助手");

        AiChatResult response = service.chat("你好");

        assertEquals("主模型响应", response.getContent());
        assertEquals("primary", response.getModelAlias());
        assertFalse(response.getFallbackUsed());
        assertEquals(1, calls.get());
    }

    @Test
    void shouldLoadSystemPromptFromAnnotationResource() {
        String prompt = AiGatewayChatService.resolveSystemPromptFromAnnotation();

        assertTrue(prompt.contains("企业级 AI 超级智能体助手"));
    }

    private static class SuccessChatModel implements ChatModel {

        private final String content;

        private SuccessChatModel(String content) {
            this.content = content;
        }

        @Override
        public ChatResponse doChat(ChatRequest request) {
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from(content))
                    .modelName("fallback-model")
                    .tokenUsage(new TokenUsage(1, 2, 3))
                    .build();
        }
    }

    private static class FailingChatModel implements ChatModel {

        @Override
        public ChatResponse doChat(ChatRequest request) {
            throw new RuntimeException("upstream timeout Authorization: Bearer secret-token");
        }
    }

    private static class CountingSuccessChatModel implements ChatModel {

        private final String modelName;

        private final String content;

        private final AtomicInteger calls;

        private CountingSuccessChatModel(String modelName, String content, AtomicInteger calls) {
            this.modelName = modelName;
            this.content = content;
            this.calls = calls;
        }

        @Override
        public ChatResponse doChat(ChatRequest request) {
            calls.incrementAndGet();
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from(content))
                    .modelName(modelName)
                    .build();
        }
    }
}
