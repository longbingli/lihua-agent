package com.bingli.lihuaAgent.module.ai.config;

import com.bingli.lihuaAgent.module.ai.api.AiChatService;
import com.bingli.lihuaAgent.module.ai.api.AiStreamChatService;
import com.bingli.lihuaAgent.module.ai.core.AiGatewayChatService;
import com.bingli.lihuaAgent.module.ai.core.AiModelClient;
import com.bingli.lihuaAgent.module.ai.core.AiSseStreamChatService;
import com.bingli.lihuaAgent.module.ai.core.AiStreamingModelClient;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
@Configuration
@EnableConfigurationProperties(AiModuleProperties.class)
public class AiModuleConfiguration {

    @Bean
    public List<AiModelClient> aiModelClients(AiModuleProperties properties) {
        List<AiModelClient> clients = properties.enabledModels().stream()
                .map(model -> buildChatClient(model, properties))
                .toList();
        log.info("AI 模块已加载 {} 个启用模型", clients.size());
        return clients;
    }

    @Bean
    public List<AiStreamingModelClient> aiStreamingModelClients(AiModuleProperties properties) {
        return properties.enabledModels().stream()
                .map(model -> buildStreamingClient(model, properties))
                .toList();
    }

    @Bean
    public AiChatService aiChatService(List<AiModelClient> aiModelClients) {
        return new AiGatewayChatService(aiModelClients, AiGatewayChatService.resolveSystemPromptFromAnnotation());
    }

    @Bean
    public AiStreamChatService aiStreamChatService(List<AiStreamingModelClient> aiStreamingModelClients,
                                                   @Qualifier("aiSseExecutor") ExecutorService executorService) {
        return new AiSseStreamChatService(
                aiStreamingModelClients,
                AiGatewayChatService.resolveSystemPromptFromAnnotation(),
                executorService
        );
    }

    private AiModelClient buildChatClient(AiModuleProperties.ModelConfig model, AiModuleProperties properties) {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl(model.getBaseUrl())
                .apiKey(model.getApiKey())
                .modelName(model.getModelName())
                .timeout(model.getTimeout())
                .maxRetries(model.getMaxRetries())
                .logRequests(properties.isLogRequests())
                .logResponses(properties.isLogResponses())
                .build();
        return new AiModelClient(model.getAlias(), model.getModelName(), chatModel);
    }

    private AiStreamingModelClient buildStreamingClient(AiModuleProperties.ModelConfig model, AiModuleProperties properties) {
        OpenAiStreamingChatModel streamingChatModel = OpenAiStreamingChatModel.builder()
                .baseUrl(model.getBaseUrl())
                .apiKey(model.getApiKey())
                .modelName(model.getModelName())
                .timeout(model.getTimeout())
                .logRequests(properties.isLogRequests())
                .logResponses(properties.isLogResponses())
                .build();
        return new AiStreamingModelClient(model.getAlias(), model.getModelName(), streamingChatModel);
    }
}
