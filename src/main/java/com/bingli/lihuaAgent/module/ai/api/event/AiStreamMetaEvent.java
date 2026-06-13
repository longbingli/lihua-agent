package com.bingli.lihuaAgent.module.ai.api.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiStreamMetaEvent {

    private String modelAlias;

    private String modelName;

    private Boolean fallbackUsed;

    private Long costMs;
}
