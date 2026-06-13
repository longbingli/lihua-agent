package com.bingli.lihuaAgent.config;


import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;


import java.net.URI;

/**
 * S3 兼容对象存储客户端配置
 */
@Configuration
@ConfigurationProperties(prefix = "s3.client")
@Data
public class S3ClientConfig {

    private String endpoint;

    private String region;

    private String bucket;

    private String accessKey;

    private String secretKey;

    private String publicHost;

    @Bean
    public S3Client s3Client() {
        if (StringUtils.isBlank(accessKey)) {
            return null;
        }
        S3ClientBuilderFactory factory = new S3ClientBuilderFactory();
        return factory.build(this);
    }

    private static class S3ClientBuilderFactory {

        private S3Client build(S3ClientConfig config) {
            software.amazon.awssdk.services.s3.S3ClientBuilder builder = S3Client.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(config.getAccessKey(), config.getSecretKey())))
                    .region(Region.of(StringUtils.defaultIfBlank(config.getRegion(), "us-east-1")))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build());
            if (StringUtils.isNotBlank(config.getEndpoint())) {
                builder.endpointOverride(URI.create(config.getEndpoint()));
            }
            return builder.build();
        }
    }
}
