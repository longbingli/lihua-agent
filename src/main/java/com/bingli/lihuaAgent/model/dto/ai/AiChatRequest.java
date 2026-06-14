package com.bingli.lihuaAgent.model.dto.ai;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AiChatRequest implements Serializable {

    private String message;

    private String systemPrompt;

    /**
     * 多模态图片输入，兼容旧版字段。
     */
    private List<String> images;

    /**
     * 图片识别细节等级，可选：low / medium / high / ultra-high / auto。
     */
    private String imageDetailLevel;

    /**
     * 新版临时附件请求。
     */
    private List<AiAttachmentRequest> attachments;
}
