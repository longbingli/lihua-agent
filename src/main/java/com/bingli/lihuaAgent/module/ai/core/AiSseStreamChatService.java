package com.bingli.lihuaAgent.module.ai.core;

import com.bingli.lihuaAgent.common.ErrorCode;
import com.bingli.lihuaAgent.exception.BusinessException;
import com.bingli.lihuaAgent.model.dto.ai.AiAttachmentRequest;
import com.bingli.lihuaAgent.model.enums.AiAttachmentTypeEnum;
import com.bingli.lihuaAgent.model.vo.ai.AiAttachmentContent;
import com.bingli.lihuaAgent.module.ai.api.AiStreamChatService;
import com.bingli.lihuaAgent.module.ai.api.event.AiStreamDoneEvent;
import com.bingli.lihuaAgent.module.ai.api.event.AiStreamErrorEvent;
import com.bingli.lihuaAgent.module.ai.api.event.AiStreamMetaEvent;
import com.bingli.lihuaAgent.module.ai.api.event.AiStreamTokenEvent;
import com.bingli.lihuaAgent.module.ai.util.AiErrorSanitizer;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RequiredArgsConstructor
public class AiSseStreamChatService implements AiStreamChatService {

    private static final Long SSE_TIMEOUT = 180_000L;
    private static final long STREAM_WAIT_SECONDS = 60L;
    private static final long HEARTBEAT_INITIAL_DELAY_SECONDS = 15L;
    private static final long HEARTBEAT_INTERVAL_SECONDS = 15L;

    private final List<AiStreamingModelClient> streamingModelClients;
    private final String defaultSystemPrompt;
    private final ExecutorService executorService;
    private final AiAttachmentResolver aiAttachmentResolver;
    private final ScheduledExecutorService heartbeatScheduler = new ScheduledThreadPoolExecutor(2);

    @Override
    public SseEmitter streamChat(String userMessage, List<String> imageUrls, List<AiAttachmentRequest> attachments,
                                 String systemPrompt, String imageDetailLevel, Long loginUserId) {
        validateInput(userMessage, imageUrls, attachments);
        if (streamingModelClients == null || streamingModelClients.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_MODEL_NOT_CONFIGURED, "未配置可用的流式 AI 模型");
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        registerLifecycleCallbacks(emitter);
        ChatRequest chatRequest = buildChatRequest(userMessage, imageUrls, attachments, systemPrompt, imageDetailLevel, loginUserId);

        ScheduledFuture<?> heartbeatFuture = heartbeatScheduler.scheduleAtFixedRate(
                () -> sendHeartbeat(emitter),
                HEARTBEAT_INITIAL_DELAY_SECONDS,
                HEARTBEAT_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );

        executorService.submit(() -> {
            try {
                executeStreaming(chatRequest, emitter);
            } finally {
                heartbeatFuture.cancel(false);
            }
        });
        return emitter;
    }

    private void executeStreaming(ChatRequest chatRequest, SseEmitter emitter) {
        Throwable lastError = null;
        long totalStart = System.currentTimeMillis();

        for (int index = 0; index < streamingModelClients.size(); index++) {
            AiStreamingModelClient client = streamingModelClients.get(index);
            try {
                streamWithClient(chatRequest, emitter, client, index > 0, totalStart);
                return;
            } catch (Exception e) {
                lastError = e;
                log.warn("AI 流式调用失败，alias={}, modelName={}, errorType={}, errorMsg={}",
                        client.getAlias(), client.getModelName(), e.getClass().getSimpleName(), AiErrorSanitizer.sanitize(e));
            }
        }

        completeWithError(emitter, lastError == null
                ? new BusinessException(ErrorCode.AI_FALLBACK_EXHAUSTED, "AI 流式服务暂不可用，请稍后重试")
                : AiErrorSanitizer.toBusinessException(lastError, ErrorCode.AI_FALLBACK_EXHAUSTED, "AI 流式服务暂不可用，请稍后重试"));
    }

    private void streamWithClient(ChatRequest chatRequest, SseEmitter emitter, AiStreamingModelClient client,
                                  boolean fallbackUsed, long totalStart) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        AtomicReference<ChatResponse> responseRef = new AtomicReference<>();
        StringBuilder fullContent = new StringBuilder();

        client.getStreamingChatModel().chat(chatRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                if (!StringUtils.hasText(partialResponse)) {
                    return;
                }
                fullContent.append(partialResponse);
                sendEvent(emitter, "token", new AiStreamTokenEvent(partialResponse));
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                responseRef.set(response);
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                errorRef.set(error);
                latch.countDown();
            }
        });

        boolean finished = latch.await(STREAM_WAIT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            throw new BusinessException(ErrorCode.AI_REQUEST_TIMEOUT, "AI 流式响应超时");
        }
        if (errorRef.get() != null) {
            throw AiErrorSanitizer.toBusinessException(errorRef.get(), ErrorCode.AI_STREAM_INTERRUPTED, "AI 流式响应中断");
        }

        ChatResponse response = responseRef.get();
        if (!StringUtils.hasText(fullContent.toString())) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_EMPTY, "AI 流式响应为空");
        }
        long costMs = System.currentTimeMillis() - totalStart;
        TokenUsage tokenUsage = response == null ? null : response.tokenUsage();
        sendEvent(emitter, "done", AiStreamDoneEvent.builder()
                .content(fullContent.toString())
                .modelAlias(client.getAlias())
                .modelName(resolveModelName(response, client))
                .fallbackUsed(fallbackUsed)
                .costMs(costMs)
                .inputTokens(tokenUsage == null ? null : tokenUsage.inputTokenCount())
                .outputTokens(tokenUsage == null ? null : tokenUsage.outputTokenCount())
                .totalTokens(tokenUsage == null ? null : tokenUsage.totalTokenCount())
                .build());
        sendEvent(emitter, "meta", AiStreamMetaEvent.builder()
                .modelAlias(client.getAlias())
                .modelName(resolveModelName(response, client))
                .fallbackUsed(fallbackUsed)
                .costMs(costMs)
                .build());
        log.info("AI 流式调用成功，alias={}, modelName={}, fallbackUsed={}, costMs={}",
                client.getAlias(), client.getModelName(), fallbackUsed, costMs);
        emitter.complete();
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

    private void validateInput(String userMessage, List<String> imageUrls, List<AiAttachmentRequest> attachments) {
        boolean hasMessage = StringUtils.hasText(userMessage);
        boolean hasImages = imageUrls != null && imageUrls.stream().anyMatch(StringUtils::hasText);
        boolean hasAttachments = !CollectionUtils.isEmpty(attachments);
        if (!hasMessage && !hasImages && !hasAttachments) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容和附件不能同时为空");
        }
    }

    private void registerLifecycleCallbacks(SseEmitter emitter) {
        emitter.onCompletion(() -> log.info("SSE 连接关闭"));
        emitter.onTimeout(() -> log.warn("SSE 连接超时"));
        emitter.onError(throwable -> log.warn("SSE 连接异常，errorMsg={}", AiErrorSanitizer.sanitize(throwable)));
    }

    private void sendHeartbeat(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().comment("keepalive"));
        } catch (IOException e) {
            log.debug("SSE 心跳发送失败，errorMsg={}", AiErrorSanitizer.sanitize(e));
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.AI_STREAM_SEND_FAILED, "发送 SSE 事件失败");
        }
    }

    private void completeWithError(SseEmitter emitter, Throwable throwable) {
        BusinessException businessException = AiErrorSanitizer.toBusinessException(
                throwable,
                ErrorCode.AI_FALLBACK_EXHAUSTED,
                "AI 流式服务暂不可用，请稍后重试"
        );
        try {
            sendEvent(emitter, "error", new AiStreamErrorEvent(
                    businessException.getCode(),
                    AiErrorSanitizer.sanitize(businessException)
            ));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(new BusinessException(ErrorCode.AI_STREAM_INTERRUPTED, "AI 流式服务暂不可用，请稍后重试"));
        }
    }

    private String resolveModelName(ChatResponse response, AiStreamingModelClient client) {
        if (response != null && StringUtils.hasText(response.modelName())) {
            return response.modelName();
        }
        return client.getModelName();
    }
}
