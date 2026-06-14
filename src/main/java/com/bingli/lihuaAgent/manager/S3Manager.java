package com.bingli.lihuaAgent.manager;


import com.bingli.lihuaAgent.common.ErrorCode;
import com.bingli.lihuaAgent.config.S3ClientConfig;
import com.bingli.lihuaAgent.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * S3 兼容对象存储操作
 */
@Component
public class S3Manager {

    @Autowired(required = false)
    private S3ClientConfig s3ClientConfig;

    @Autowired(required = false)
    private S3Client s3Client;

    private void checkS3Available() {
        if (s3Client == null || s3ClientConfig == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件存储服务未配置");
        }
    }

    public PutObjectResponse putObject(String key, File file) {
        checkS3Available();
        String normalizedKey = normalizeKey(key);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3ClientConfig.getBucket())
                .key(normalizedKey)
                .build();
        return s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
    }

    public PutObjectResponse putObjectTemp(String key, File file, String contentType, Map<String, String> metadata) {
        checkS3Available();
        String normalizedKey = normalizeKey(key);
        Map<String, String> safeMetadata = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3ClientConfig.getBucket())
                .key(normalizedKey)
                .contentType(contentType)
                .metadata(safeMetadata)
                .build();
        return s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
    }

    public byte[] getObjectBytes(String key) {
        checkS3Available();
        try {
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(s3ClientConfig.getBucket())
                    .key(normalizeKey(key))
                    .build());
            return response.asByteArray();
        } catch (NoSuchKeyException e) {
            throw new BusinessException(ErrorCode.AI_ATTACHMENT_INVALID, "附件不存在");
        }
    }

    public HeadObjectResponse headObject(String key) {
        checkS3Available();
        try {
            return s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(s3ClientConfig.getBucket())
                    .key(normalizeKey(key))
                    .build());
        } catch (NoSuchKeyException e) {
            throw new BusinessException(ErrorCode.AI_ATTACHMENT_INVALID, "附件不存在");
        }
    }

    public String buildPublicUrl(String key) {
        checkS3Available();
        String normalizedKey = normalizeKey(key);
        String publicHost = StringUtils.removeEnd(s3ClientConfig.getPublicHost(), "/");
        return publicHost + "/" + normalizedKey;
    }

    public String normalizeKey(String key) {
        return StringUtils.removeStart(key, "/");
    }
}
