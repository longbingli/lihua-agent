package com.bingli.lihuaAgent.model.vo.ai;

import com.bingli.lihuaAgent.model.enums.AiAttachmentTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
public class AiAttachmentContent implements Serializable {

    private String fileId;

    private String storageKey;

    private String url;

    private String fileName;

    private String mimeType;

    private Long size;

    private Instant expiresAt;

    private AiAttachmentTypeEnum type;

    private String textContent;
}
