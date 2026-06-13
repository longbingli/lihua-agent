package com.bingli.lihuaAgent.model.dto.ai;

import lombok.Data;

import java.io.Serializable;

@Data
public class AiChatRequest implements Serializable {

    private String message;

    private String systemPrompt;
}
