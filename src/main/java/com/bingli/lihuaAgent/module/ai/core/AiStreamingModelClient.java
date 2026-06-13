package com.bingli.lihuaAgent.module.ai.core;

import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiStreamingModelClient {

    private String alias;

    private String modelName;

    private StreamingChatModel streamingChatModel;
}
