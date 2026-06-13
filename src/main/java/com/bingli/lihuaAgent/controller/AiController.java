package com.bingli.lihuaAgent.controller;

import com.bingli.lihuaAgent.common.BaseResponse;
import com.bingli.lihuaAgent.common.ResultUtils;
import com.bingli.lihuaAgent.model.dto.ai.AiChatRequest;
import com.bingli.lihuaAgent.model.vo.ai.AiChatResponse;
import com.bingli.lihuaAgent.module.ai.api.AiChatService;
import com.bingli.lihuaAgent.module.ai.api.AiStreamChatService;
import com.bingli.lihuaAgent.module.ai.api.model.AiChatResult;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiChatService aiChatService;

    private final AiStreamChatService aiStreamChatService;

    @PostMapping("/chat")
    public BaseResponse<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        AiChatResult result = aiChatService.chat(request.getMessage());
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

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody AiChatRequest request, HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        return aiStreamChatService.streamChat(request.getMessage());
    }
}
