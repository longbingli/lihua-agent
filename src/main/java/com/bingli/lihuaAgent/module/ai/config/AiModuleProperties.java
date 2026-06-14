package com.bingli.lihuaAgent.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "ai.models")
public class AiModuleProperties {

    private boolean logRequests = false;

    private boolean logResponses = false;

    private Attachment attachment = new Attachment();

    private ModelConfig primary = new ModelConfig();

    private List<ModelConfig> fallbacks = new ArrayList<>();

    public List<ModelConfig> enabledModels() {
        List<ModelConfig> models = new ArrayList<>();
        addIfEnabled(models, primary);
        fallbacks.forEach(model -> addIfEnabled(models, model));
        return models;
    }

    private void addIfEnabled(List<ModelConfig> models, ModelConfig model) {
        if (model == null || !model.isEnabled()) {
            return;
        }
        model.validate();
        models.add(model);
    }

    @Data
    public static class Attachment {

        @DurationUnit(ChronoUnit.MINUTES)
        private Duration ttl = Duration.ofHours(1);

        private int maxDocumentChars = 20000;

        private int maxImageSizeMb = 10;

        private int maxTextSizeMb = 5;

        private int maxPdfSizeMb = 20;

        public void validate() {
            if (ttl.compareTo(Duration.ofMinutes(15)) < 0 || ttl.compareTo(Duration.ofHours(24)) > 0) {
                throw new IllegalStateException("AI 临时附件 ttl 必须在 15 分钟到 24 小时之间");
            }
            if (maxDocumentChars <= 0) {
                throw new IllegalStateException("AI 文档最大注入字符数必须大于 0");
            }
        }
    }

    @Data
    public static class ModelConfig {

        private boolean enabled = false;

        private String alias = "primary";

        private String baseUrl;

        private String apiKey;

        private String modelName;

        @DurationUnit(ChronoUnit.SECONDS)
        private Duration timeout = Duration.ofSeconds(30);

        private Integer maxRetries = 1;

        public void validate() {
            if (!StringUtils.hasText(alias)) {
                throw new IllegalStateException("AI 模型 alias 不能为空");
            }
            if (!StringUtils.hasText(baseUrl)) {
                throw new IllegalStateException("AI 模型 " + alias + " 的 baseUrl 不能为空");
            }
            if (!StringUtils.hasText(apiKey)) {
                throw new IllegalStateException("AI 模型 " + alias + " 的 apiKey 不能为空");
            }
            if (!StringUtils.hasText(modelName)) {
                throw new IllegalStateException("AI 模型 " + alias + " 的 modelName 不能为空");
            }
        }
    }
}
