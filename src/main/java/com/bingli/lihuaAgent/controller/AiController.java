package com.bingli.lihuaAgent.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.bingli.lihuaAgent.common.BaseResponse;
import com.bingli.lihuaAgent.common.ResultUtils;
import com.bingli.lihuaAgent.model.dto.ai.AiChatRequest;
import com.bingli.lihuaAgent.model.vo.ai.AiChatResponse;
import com.bingli.lihuaAgent.module.ai.api.AiChatService;
import com.bingli.lihuaAgent.module.ai.api.AiStreamChatService;
import com.bingli.lihuaAgent.module.ai.api.model.AiChatResult;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final AiChatService aiChatService;
    private final AiStreamChatService aiStreamChatService;

    @SaCheckLogin
    @PostMapping("/chat")
    public BaseResponse<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        validateRequest(request);
        Long loginUserId = StpUtil.getLoginIdAsLong();
        AiChatResult result = aiChatService.chat(
                request.getMessage(),
                request.getImages(),
                request.getAttachments(),
                request.getSystemPrompt(),
                request.getImageDetailLevel(),
                loginUserId
        );
        return ResultUtils.success(AiChatResponse.builder()
                .content(result.getContent())
                .modelAlias(result.getModelAlias())
                .modelName(result.getModelName())
                .fallbackUsed(result.getFallbackUsed())
                .costMs(result.getCostMs())
                .inputTokens(result.getInputTokens())
                .outputTokens(result.getOutputTokens())
                .totalTokens(result.getTotalTokens())
                .build());
    }

    @SaCheckLogin
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<?> streamChat(@RequestBody AiChatRequest request, HttpServletResponse response) {
        validateRequest(request);
        Long loginUserId = StpUtil.getLoginIdAsLong();
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Pragma", "no-cache");
        SseEmitter emitter = aiStreamChatService.streamChat(
                request.getMessage(),
                request.getImages(),
                request.getAttachments(),
                request.getSystemPrompt(),
                request.getImageDetailLevel(),
                loginUserId
        );
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }

    private void validateRequest(AiChatRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        boolean hasMessage = StringUtils.hasText(request.getMessage());
        boolean hasImages = !CollectionUtils.isEmpty(request.getImages())
                && request.getImages().stream().anyMatch(StringUtils::hasText);
        boolean hasAttachments = !CollectionUtils.isEmpty(request.getAttachments());
        if (!hasMessage && !hasImages && !hasAttachments) {
            throw new IllegalArgumentException("消息内容和附件不能同时为空");
        }
    }
}
