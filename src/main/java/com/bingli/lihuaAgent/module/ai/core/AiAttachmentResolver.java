package com.bingli.lihuaAgent.module.ai.core;

import com.bingli.lihuaAgent.common.ErrorCode;
import com.bingli.lihuaAgent.exception.BusinessException;
import com.bingli.lihuaAgent.manager.S3Manager;
import com.bingli.lihuaAgent.model.dto.ai.AiAttachmentRequest;
import com.bingli.lihuaAgent.model.enums.AiAttachmentTypeEnum;
import com.bingli.lihuaAgent.model.vo.ai.AiAttachmentContent;
import com.bingli.lihuaAgent.module.ai.config.AiModuleProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AiAttachmentResolver {

    private static final String TEMP_AI_PREFIX = "temp/ai/";

    private final S3Manager s3Manager;
    private final AiModuleProperties aiModuleProperties;

    public List<AiAttachmentContent> resolve(List<AiAttachmentRequest> attachments, Long loginUserId) {
        if (CollectionUtils.isEmpty(attachments)) {
            return List.of();
        }
        aiModuleProperties.getAttachment().validate();
        List<AiAttachmentContent> results = new ArrayList<>();
        for (AiAttachmentRequest attachment : attachments) {
            results.add(resolveOne(attachment, loginUserId));
        }
        return results;
    }

    private AiAttachmentContent resolveOne(AiAttachmentRequest attachment, Long loginUserId) {
        if (attachment == null || !org.springframework.util.StringUtils.hasText(attachment.getStorageKey())) {
            throw new BusinessException(ErrorCode.AI_ATTACHMENT_INVALID, "附件 storageKey 不能为空");
        }
        String storageKey = s3Manager.normalizeKey(attachment.getStorageKey());
        if (!storageKey.startsWith(TEMP_AI_PREFIX)) {
            throw new BusinessException(ErrorCode.AI_ATTACHMENT_INVALID, "仅支持引用 AI 临时附件");
        }

        HeadObjectResponse head = s3Manager.headObject(storageKey);
        Map<String, String> metadata = head.metadata();
        validateOwnership(metadata, loginUserId);
        Instant expiresAt = parseExpiresAt(metadata);
        if (Instant.now().isAfter(expiresAt)) {
            throw new BusinessException(ErrorCode.AI_ATTACHMENT_EXPIRED, "AI 附件已过期，请重新上传");
        }

        AiAttachmentTypeEnum type = resolveType(attachment, metadata);
        byte[] contentBytes = type == AiAttachmentTypeEnum.IMAGE ? null : s3Manager.getObjectBytes(storageKey);
        String textContent = switch (type) {
            case IMAGE -> null;
            case TEXT -> parseText(contentBytes);
            case PDF -> parsePdf(contentBytes);
        };

        if (textContent != null) {
            textContent = truncateIfNeeded(textContent);
        }

        return AiAttachmentContent.builder()
                .fileId(defaultIfBlank(attachment.getFileId(), metadata.get("file-id")))
                .storageKey(storageKey)
                .url(defaultIfBlank(attachment.getUrl(), s3Manager.buildPublicUrl(storageKey)))
                .fileName(defaultIfBlank(attachment.getFileName(), metadata.get("file-name")))
                .mimeType(defaultIfBlank(attachment.getMimeType(), head.contentType()))
                .size(head.contentLength())
                .expiresAt(expiresAt)
                .type(type)
                .textContent(textContent)
                .build();
    }

    private void validateOwnership(Map<String, String> metadata, Long loginUserId) {
        String ownerId = metadata.get("owner-id");
        if (!StringUtils.equals(String.valueOf(loginUserId), ownerId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权访问该临时附件");
        }
    }

    private Instant parseExpiresAt(Map<String, String> metadata) {
        String expiresAt = metadata.get("expires-at");
        if (!org.springframework.util.StringUtils.hasText(expiresAt)) {
            throw new BusinessException(ErrorCode.AI_ATTACHMENT_INVALID, "附件缺少过期时间");
        }
        return Instant.parse(expiresAt);
    }

    private AiAttachmentTypeEnum resolveType(AiAttachmentRequest attachment, Map<String, String> metadata) {
        AiAttachmentTypeEnum type = AiAttachmentTypeEnum.getByValue(attachment.getType());
        if (type == null) {
            type = AiAttachmentTypeEnum.getByValue(metadata.get("attachment-type"));
        }
        if (type == null) {
            throw new BusinessException(ErrorCode.AI_ATTACHMENT_UNSUPPORTED_TYPE, "附件类型不支持");
        }
        return type;
    }

    private String parseText(byte[] contentBytes) {
        try {
            return new String(contentBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_ATTACHMENT_PARSE_FAILED, "文本附件解析失败");
        }
    }

    private String parsePdf(byte[] contentBytes) {
        try (PDDocument document = Loader.loadPDF(contentBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.AI_ATTACHMENT_PARSE_FAILED, "PDF 附件解析失败");
        }
    }

    private String truncateIfNeeded(String textContent) {
        String normalized = textContent == null ? "" : textContent.trim();
        int maxChars = aiModuleProperties.getAttachment().getMaxDocumentChars();
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        return "以下内容已截断：\n" + normalized.substring(0, maxChars);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return org.springframework.util.StringUtils.hasText(value) ? value : defaultValue;
    }
}
