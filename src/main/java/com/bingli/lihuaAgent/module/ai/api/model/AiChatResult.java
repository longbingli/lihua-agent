package com.bingli.lihuaAgent.module.ai.api.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class AiChatResult implements Serializable {

    private String content;

    private String modelAlias;

    private String modelName;

    private Boolean fallbackUsed;

    private Long costMs;

    private Integer inputTokens;

    private Integer outputTokens;

    private Integer totalTokens;
}
