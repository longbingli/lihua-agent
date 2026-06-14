package com.bingli.lihuaAgent.model.vo.ai;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
public class AiUploadFileResponse implements Serializable {

    private String fileId;

    private String fileName;

    private String fileType;

    private String mimeType;

    private Long size;

    private String url;

    private Instant expiresAt;

    private String storageKey;
}
