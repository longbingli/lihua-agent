package com.bingli.lihuaAgent.module.ai.api;

import com.bingli.lihuaAgent.model.dto.ai.AiAttachmentRequest;
import dev.langchain4j.service.SystemMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AiStreamChatService {

    @SystemMessage(fromResource = "system-prompt.txt")
    SseEmitter streamChat(String userMessage, List<String> imageUrls, List<AiAttachmentRequest> attachments,
                          String systemPrompt, String imageDetailLevel, Long loginUserId);
}
