package com.bingli.lihuaAgent.module.ai.api;

import com.bingli.lihuaAgent.module.ai.api.model.AiChatResult;
import dev.langchain4j.service.SystemMessage;


public interface AiChatService {

    @SystemMessage(fromResource = "system-prompt.txt")
    AiChatResult chat(String userMessage);


}
