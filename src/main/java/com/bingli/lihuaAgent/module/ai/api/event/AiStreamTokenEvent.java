package com.bingli.lihuaAgent.module.ai.api.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiStreamTokenEvent {

    private String content;
}
