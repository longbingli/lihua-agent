package com.bingli.lihuaAgent.module.ai.core;

import dev.langchain4j.model.chat.ChatModel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiModelClient {

    private String alias;

    private String modelName;

    private ChatModel chatModel;
}
