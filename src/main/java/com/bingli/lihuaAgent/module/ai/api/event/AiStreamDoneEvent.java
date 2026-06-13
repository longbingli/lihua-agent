package com.bingli.lihuaAgent.module.ai.api.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiStreamDoneEvent {

    private String content;

    private String modelAlias;

    private String modelName;

    private Boolean fallbackUsed;

    private Long costMs;

    private Integer inputTokens;

    private Integer outputTokens;

    private Integer totalTokens;
}
