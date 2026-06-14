package com.bingli.lihuaAgent.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import com.bingli.lihuaAgent.common.BaseResponse;
import com.bingli.lihuaAgent.common.ErrorCode;
import com.bingli.lihuaAgent.common.ResultUtils;
import com.bingli.lihuaAgent.exception.BusinessException;
import com.bingli.lihuaAgent.manager.S3Manager;
import com.bingli.lihuaAgent.model.dto.ai.AiUploadTempFileRequest;
import com.bingli.lihuaAgent.model.enums.AiAttachmentTypeEnum;
import com.bingli.lihuaAgent.model.vo.ai.AiUploadFileResponse;
import com.bingli.lihuaAgent.module.ai.config.AiModuleProperties;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai/files")
@RequiredArgsConstructor
@Slf4j
public class AiFileController {

    private static final List<String> IMAGE_SUFFIXES = List.of("jpeg", "jpg", "png", "webp");
    private static final List<String> TEXT_SUFFIXES = List.of("txt", "md", "json", "csv");
    private static final List<String> PDF_SUFFIXES = List.of("pdf");

    @Resource
    private S3Manager s3Manager;

    private final AiModuleProperties aiModuleProperties;

    @SaCheckLogin
    @PostMapping("/upload-temp")
    public BaseResponse<AiUploadFileResponse> uploadTempFile(@RequestPart("file") MultipartFile multipartFile,
                                                             AiUploadTempFileRequest uploadRequest) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        }
        AiAttachmentTypeEnum typeEnum = AiAttachmentTypeEnum.getByValue(uploadRequest == null ? null : uploadRequest.getType());
        if (typeEnum == null) {
            throw new BusinessException(ErrorCode.AI_ATTACHMENT_UNSUPPORTED_TYPE, "仅支持 image、text、pdf 类型");
        }
        validateFile(multipartFile, typeEnum);

        Long loginUserId = StpUtil.getLoginIdAsLong();
        Instant expiresAt = Instant.now().plus(aiModuleProperties.getAttachment().getTtl());
        String fileId = RandomStringUtils.randomAlphanumeric(16);
        String originalFilename = StringUtils.hasText(multipartFile.getOriginalFilename()) ? multipartFile.getOriginalFilename() : fileId;
        String filename = fileId + "-" + originalFilename;
        String day = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.BASIC_ISO_DATE);
        String storageKey = String.format("temp/ai/%s/%s/%s", loginUserId, day, filename);

        File tempFile = null;
        try {
            tempFile = File.createTempFile("ai-upload-", "-" + filename);
            multipartFile.transferTo(tempFile);
            Map<String, String> metadata = new HashMap<>();
            metadata.put("owner-id", String.valueOf(loginUserId));
            metadata.put("attachment-type", typeEnum.getValue());
            metadata.put("expires-at", expiresAt.toString());
            metadata.put("file-id", fileId);
            metadata.put("file-name", originalFilename);
            s3Manager.putObjectTemp(storageKey, tempFile, multipartFile.getContentType(), metadata);
            return ResultUtils.success(AiUploadFileResponse.builder()
                    .fileId(fileId)
                    .fileName(originalFilename)
                    .fileType(typeEnum.getValue())
                    .mimeType(multipartFile.getContentType())
                    .size(multipartFile.getSize())
                    .url(s3Manager.buildPublicUrl(storageKey))
                    .expiresAt(expiresAt)
                    .storageKey(storageKey)
                    .build());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("ai temp file upload error, storageKey={}", storageKey, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "临时附件上传失败");
        } finally {
            if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
                log.warn("temp ai file delete failed, storageKey={}", storageKey);
            }
        }
    }

    private void validateFile(MultipartFile multipartFile, AiAttachmentTypeEnum typeEnum) {
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename()).toLowerCase();
        long fileSize = multipartFile.getSize();
        int limitMb;
        List<String> allowSuffixes;
        switch (typeEnum) {
            case IMAGE -> {
                limitMb = aiModuleProperties.getAttachment().getMaxImageSizeMb();
                allowSuffixes = IMAGE_SUFFIXES;
            }
            case TEXT -> {
                limitMb = aiModuleProperties.getAttachment().getMaxTextSizeMb();
                allowSuffixes = TEXT_SUFFIXES;
            }
            case PDF -> {
                limitMb = aiModuleProperties.getAttachment().getMaxPdfSizeMb();
                allowSuffixes = PDF_SUFFIXES;
            }
            default -> throw new BusinessException(ErrorCode.AI_ATTACHMENT_UNSUPPORTED_TYPE, "附件类型不支持");
        }
        if (!allowSuffixes.contains(fileSuffix)) {
            throw new BusinessException(ErrorCode.AI_ATTACHMENT_UNSUPPORTED_TYPE, "文件类型不支持");
        }
        long maxBytes = limitMb * 1024L * 1024L;
        if (fileSize > maxBytes) {
            throw new BusinessException(ErrorCode.AI_ATTACHMENT_TOO_LARGE, "附件大小不能超过 " + limitMb + "MB");
        }
    }
}
