package com.bingli.lihuaAgent.module.ai.core;

import com.bingli.lihuaAgent.common.ErrorCode;
import com.bingli.lihuaAgent.exception.BusinessException;
import com.bingli.lihuaAgent.model.dto.ai.AiAttachmentRequest;
import com.bingli.lihuaAgent.model.enums.AiAttachmentTypeEnum;
import com.bingli.lihuaAgent.model.vo.ai.AiAttachmentContent;
import com.bingli.lihuaAgent.module.ai.api.AiChatService;
import com.bingli.lihuaAgent.module.ai.api.model.AiChatResult;
import com.bingli.lihuaAgent.module.ai.util.AiErrorSanitizer;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@RequiredArgsConstructor
public class AiGatewayChatService implements AiChatService {

    private final List<AiModelClient> aiModelClients;
    private final String defaultSystemPrompt;
    private final AiAttachmentResolver aiAttachmentResolver;

    @Override
    public AiChatResult chat(String userMessage, List<String> imageUrls, List<AiAttachmentRequest> attachments,
                             String systemPrompt, String imageDetailLevel, Long loginUserId) {
        validateInput(userMessage, imageUrls, attachments);
        if (aiModelClients == null || aiModelClients.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_MODEL_NOT_CONFIGURED, "未配置可用的 AI 模型");
        }

        ChatRequest chatRequest = buildChatRequest(userMessage, imageUrls, attachments, systemPrompt, imageDetailLevel, loginUserId);
        Throwable lastError = null;
        long totalStart = System.currentTimeMillis();

        for (int index = 0; index < aiModelClients.size(); index++) {
            AiModelClient client = aiModelClients.get(index);
            long attemptStart = System.currentTimeMillis();
            try {
                ChatResponse response = client.getChatModel().chat(chatRequest);
                if (response == null || response.aiMessage() == null || !StringUtils.hasText(response.aiMessage().text())) {
                    throw new BusinessException(ErrorCode.AI_RESPONSE_EMPTY, "AI 响应为空");
                }
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
        throw AiErrorSanitizer.toBusinessException(lastError,
                ErrorCode.AI_FALLBACK_EXHAUSTED,
                "AI 模型服务暂不可用，请稍后重试");
    }

    public static String resolveSystemPromptFromAnnotation() {
        try {
            dev.langchain4j.service.SystemMessage annotation = AiChatService.class
                    .getMethod("chat", String.class, List.class, List.class, String.class, String.class, Long.class)
                    .getAnnotation(dev.langchain4j.service.SystemMessage.class);
            if (annotation == null || !StringUtils.hasText(annotation.fromResource())) {
                return null;
            }
            ClassPathResource resource = new ClassPathResource(annotation.fromResource());
            byte[] bytes = resource.getInputStream().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8).trim();
        } catch (NoSuchMethodException | IOException e) {
            throw new BusinessException(ErrorCode.AI_SERVICE_CONFIG_INVALID, "加载 AI 系统提示词失败");
        }
    }

    private void validateInput(String userMessage, List<String> imageUrls, List<AiAttachmentRequest> attachments) {
        boolean hasMessage = StringUtils.hasText(userMessage);
        boolean hasImages = imageUrls != null && imageUrls.stream().anyMatch(StringUtils::hasText);
        boolean hasAttachments = !CollectionUtils.isEmpty(attachments);
        if (!hasMessage && !hasImages && !hasAttachments) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容和附件不能同时为空");
        }
    }

    private ChatRequest buildChatRequest(String userMessage, List<String> imageUrls, List<AiAttachmentRequest> attachments,
                                         String systemPrompt, String imageDetailLevel, Long loginUserId) {
        List<ChatMessage> messages = new ArrayList<>();
        String resolvedSystemPrompt = StringUtils.hasText(systemPrompt) ? systemPrompt : defaultSystemPrompt;
        if (StringUtils.hasText(resolvedSystemPrompt)) {
            messages.add(SystemMessage.from(resolvedSystemPrompt));
        }
        messages.add(buildUserMessage(userMessage, imageUrls, attachments, imageDetailLevel, loginUserId));
        return ChatRequest.builder().messages(messages).build();
    }

    private UserMessage buildUserMessage(String userMessage, List<String> imageUrls, List<AiAttachmentRequest> attachments,
                                         String imageDetailLevel, Long loginUserId) {
        List<Content> contents = new ArrayList<>();
        if (StringUtils.hasText(userMessage)) {
            contents.add(TextContent.from(userMessage.trim()));
        }
        ImageContent.DetailLevel detailLevel = resolveDetailLevel(imageDetailLevel);
        appendLegacyImages(contents, imageUrls, detailLevel);
        appendAttachments(contents, attachments, detailLevel, loginUserId);
        return UserMessage.from(contents);
    }

    private void appendLegacyImages(List<Content> contents, List<String> imageUrls, ImageContent.DetailLevel detailLevel) {
        if (imageUrls == null) {
            return;
        }
        for (String imageUrl : imageUrls) {
            if (StringUtils.hasText(imageUrl)) {
                contents.add(ImageContent.from(imageUrl.trim(), detailLevel));
            }
        }
    }

    private void appendAttachments(List<Content> contents, List<AiAttachmentRequest> attachments,
                                   ImageContent.DetailLevel detailLevel, Long loginUserId) {
        List<AiAttachmentContent> attachmentContents = aiAttachmentResolver.resolve(attachments, loginUserId);
        for (AiAttachmentContent attachmentContent : attachmentContents) {
            if (attachmentContent.getType() == AiAttachmentTypeEnum.IMAGE) {
                contents.add(ImageContent.from(attachmentContent.getUrl(), detailLevel));
            } else {
                contents.add(TextContent.from(buildDocumentPrompt(attachmentContent)));
            }
        }
    }

    private String buildDocumentPrompt(AiAttachmentContent attachmentContent) {
        return "附件名称：" + attachmentContent.getFileName() + "\n"
                + "附件类型：" + attachmentContent.getType().getValue() + "\n"
                + "附件内容：\n" + attachmentContent.getTextContent();
    }

    private ImageContent.DetailLevel resolveDetailLevel(String imageDetailLevel) {
        if (!StringUtils.hasText(imageDetailLevel)) {
            return ImageContent.DetailLevel.AUTO;
        }
        try {
            return ImageContent.DetailLevel.valueOf(imageDetailLevel.trim().replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "imageDetailLevel 仅支持 low、medium、high、ultra-high、auto");
        }
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
