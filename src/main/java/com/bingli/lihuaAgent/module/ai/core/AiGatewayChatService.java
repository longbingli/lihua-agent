package com.bingli.lihuaAgent.module.ai.core;

import com.bingli.lihuaAgent.common.ErrorCode;
import com.bingli.lihuaAgent.exception.BusinessException;
import com.bingli.lihuaAgent.module.ai.api.AiChatService;
import com.bingli.lihuaAgent.module.ai.api.model.AiChatResult;
import com.bingli.lihuaAgent.module.ai.util.AiErrorSanitizer;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class AiGatewayChatService implements AiChatService {

    private final List<AiModelClient> aiModelClients;

    private final String systemPrompt;

    @Override
    public AiChatResult chat(String userMessage) {
        validateUserMessage(userMessage);
        if (aiModelClients == null || aiModelClients.isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未配置可用的 AI 模型");
        }

        ChatRequest chatRequest = buildChatRequest(userMessage);
        Throwable lastError = null;
        long totalStart = System.currentTimeMillis();

        for (int index = 0; index < aiModelClients.size(); index++) {
            AiModelClient client = aiModelClients.get(index);
            long attemptStart = System.currentTimeMillis();
            try {
                ChatResponse response = client.getChatModel().chat(chatRequest);
                long totalCost = System.currentTimeMillis() - totalStart;
                log.info("AI 模块调用成功，alias={}, modelName={}, fallbackUsed={}, costMs={}",
                        client.getAlias(), client.getModelName(), index > 0, totalCost);
                return buildResult(response, client, index > 0, totalCost);
            } catch (Exception e) {
                lastError = e;
                long attemptCost = System.currentTimeMillis() - attemptStart;
                log.warn("AI 模块调用失败，alias={}, modelName={}, attemptCostMs={}, errorType={}, errorMsg={}",
                        client.getAlias(), client.getModelName(), attemptCost,
                        e.getClass().getSimpleName(), AiErrorSanitizer.sanitize(e));
            }
        }

        log.error("所有 AI 模型均调用失败，totalCostMs={}, lastErrorType={}, lastErrorMsg={}",
                System.currentTimeMillis() - totalStart,
                lastError == null ? "Unknown" : lastError.getClass().getSimpleName(),
                AiErrorSanitizer.sanitize(lastError));
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 模型服务暂不可用，请稍后重试");
    }

    public static String resolveSystemPromptFromAnnotation() {
        try {
            dev.langchain4j.service.SystemMessage annotation = AiChatService.class
                    .getMethod("chat", String.class)
                    .getAnnotation(dev.langchain4j.service.SystemMessage.class);
            if (annotation == null || !StringUtils.hasText(annotation.fromResource())) {
                return null;
            }
            ClassPathResource resource = new ClassPathResource(annotation.fromResource());
            byte[] bytes = resource.getInputStream().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8).trim();
        } catch (NoSuchMethodException | IOException e) {
            throw new IllegalStateException("加载 AI 系统提示词失败", e);
        }
    }

    private void validateUserMessage(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        }
    }

    private ChatRequest buildChatRequest(String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        if (StringUtils.hasText(systemPrompt)) {
            messages.add(SystemMessage.from(systemPrompt));
        }
        messages.add(UserMessage.from(userMessage));
        return ChatRequest.builder()
                .messages(messages)
                .build();
    }

    private AiChatResult buildResult(ChatResponse response, AiModelClient client, boolean fallbackUsed, long costMs) {
        TokenUsage tokenUsage = response.tokenUsage();
        return AiChatResult.builder()
                .content(response.aiMessage() == null ? "" : response.aiMessage().text())
                .modelAlias(client.getAlias())
                .modelName(resolveModelName(response, client))
                .fallbackUsed(fallbackUsed)
                .costMs(costMs)
                .inputTokens(tokenUsage == null ? null : tokenUsage.inputTokenCount())
                .outputTokens(tokenUsage == null ? null : tokenUsage.outputTokenCount())
                .totalTokens(tokenUsage == null ? null : tokenUsage.totalTokenCount())
                .build();
    }

    private String resolveModelName(ChatResponse response, AiModelClient client) {
        if (StringUtils.hasText(response.modelName())) {
            return response.modelName();
        }
        return client.getModelName();
    }
}
