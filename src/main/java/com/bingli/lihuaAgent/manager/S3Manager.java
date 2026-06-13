package com.bingli.lihuaAgent.manager;


import com.bingli.lihuaAgent.common.ErrorCode;
import com.bingli.lihuaAgent.config.S3ClientConfig;
import com.bingli.lihuaAgent.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.File;

/**
 * S3 兼容对象存储操作
 */
@Component
public class S3Manager {

    @Autowired(required = false)
    private S3ClientConfig s3ClientConfig;

    @Autowired(required = false)
    private S3Client s3Client;

    /**
     * 检查 S3 是否已配置
     */
    private void checkS3Available() {
        if (s3Client == null || s3ClientConfig == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件存储服务未配置");
        }
    }

    // 上传文件
    public PutObjectResponse putObject(String key, File file) {
        checkS3Available();
        String normalizedKey = normalizeKey(key);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3ClientConfig.getBucket())
                .key(normalizedKey)
                .build();
        return s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
    }

    // 获取文件外链
    public String buildPublicUrl(String key) {
        checkS3Available();
        String normalizedKey = normalizeKey(key);
        String publicHost = StringUtils.removeEnd(s3ClientConfig.getPublicHost(), "/");
        return publicHost + "/" + normalizedKey;
    }

    // 删除文件
    public String normalizeKey(String key) {
        return StringUtils.removeStart(key, "/");
    }
}
