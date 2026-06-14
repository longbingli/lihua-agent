package com.bingli.lihuaAgent.model.dto.ai;

import lombok.Data;

import java.io.Serializable;

@Data
public class AiAttachmentRequest implements Serializable {

    private String fileId;

    private String storageKey;

    private String url;

    private String type;

    private String fileName;

    private String mimeType;
}
