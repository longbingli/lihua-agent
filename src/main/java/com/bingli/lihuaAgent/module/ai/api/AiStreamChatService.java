package com.bingli.lihuaAgent.module.ai.api;

import dev.langchain4j.service.SystemMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiStreamChatService {

    @SystemMessage(fromResource = "system-prompt.txt")
    SseEmitter streamChat(String userMessage);
}
