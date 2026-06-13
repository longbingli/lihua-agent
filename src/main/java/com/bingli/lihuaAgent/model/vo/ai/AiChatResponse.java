package com.bingli.lihuaAgent.model.vo.ai;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class AiChatResponse implements Serializable {

    private String content;

    private String modelAlias;

    private String modelName;

    private Boolean fallbackUsed;

    private Long costMs;

    private Integer inputTokens;

    private Integer outputTokens;

    private Integer totalTokens;
}
