package com.bingli.lihuaAgent.module.ai.api;

import com.bingli.lihuaAgent.model.dto.ai.AiAttachmentRequest;
import com.bingli.lihuaAgent.module.ai.api.model.AiChatResult;
import dev.langchain4j.service.SystemMessage;

import java.util.List;

public interface AiChatService {

    @SystemMessage(fromResource = "system-prompt.txt")
    AiChatResult chat(String userMessage, List<String> imageUrls, List<AiAttachmentRequest> attachments,
                      String systemPrompt, String imageDetailLevel, Long loginUserId);

}
