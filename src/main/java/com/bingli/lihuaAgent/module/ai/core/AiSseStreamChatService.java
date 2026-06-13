package com.bingli.lihuaAgent.module.ai.core;

import com.bingli.lihuaAgent.common.ErrorCode;
import com.bingli.lihuaAgent.exception.BusinessException;
import com.bingli.lihuaAgent.module.ai.api.AiStreamChatService;
import com.bingli.lihuaAgent.module.ai.api.event.AiStreamDoneEvent;
import com.bingli.lihuaAgent.module.ai.api.event.AiStreamErrorEvent;
import com.bingli.lihuaAgent.module.ai.api.event.AiStreamMetaEvent;
import com.bingli.lihuaAgent.module.ai.api.event.AiStreamTokenEvent;
import com.bingli.lihuaAgent.module.ai.util.AiErrorSanitizer;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    private final String systemPrompt;
    private final ExecutorService executorService;
    private final ScheduledExecutorService heartbeatScheduler = new ScheduledThreadPoolExecutor(2);

    @Override
    public SseEmitter streamChat(String userMessage) {
        validateUserMessage(userMessage);
        if (streamingModelClients == null || streamingModelClients.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_MODEL_NOT_CONFIGURED, "未配置可用的流式 AI 模型");
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        registerLifecycleCallbacks(emitter);
        ChatRequest chatRequest = buildChatRequest(userMessage);

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

    private void validateUserMessage(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空");
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
