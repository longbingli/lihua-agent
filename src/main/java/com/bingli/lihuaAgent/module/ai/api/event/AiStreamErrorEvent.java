package com.bingli.lihuaAgent.module.ai.api.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiStreamErrorEvent {

    private Integer code;

    private String message;
}
