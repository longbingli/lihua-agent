package com.bingli.lihuaAgent.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.bingli.lihuaAgent.common.BaseResponse;
import com.bingli.lihuaAgent.common.ErrorCode;
import com.bingli.lihuaAgent.common.ResultUtils;
import com.bingli.lihuaAgent.constant.UserConstant;
import com.bingli.lihuaAgent.model.dto.ai.AiChatRequest;
import com.bingli.lihuaAgent.model.vo.LoginUserVO;
import com.bingli.lihuaAgent.model.vo.ai.AiChatResponse;
import com.bingli.lihuaAgent.module.ai.api.AiChatService;
import com.bingli.lihuaAgent.module.ai.api.AiStreamChatService;
import com.bingli.lihuaAgent.module.ai.api.model.AiChatResult;
import com.bingli.lihuaAgent.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final AiChatService aiChatService;

    private final AiStreamChatService aiStreamChatService;

    private final UserService userService;

    @SaCheckLogin
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

    @SaCheckLogin
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<?> streamChat(@RequestBody AiChatRequest request, HttpServletResponse response) {

        if (request == null || !StringUtils.hasText(request.getMessage())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ResultUtils.error(ErrorCode.PARAMS_ERROR, "消息内容不能为空"));
        }

        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Pragma", "no-cache");

        SseEmitter emitter = aiStreamChatService.streamChat(request.getMessage());
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }
}
