package com.aibookkeeping.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.web.client.RestClient;

/**
 * AI 模型配置 - DeepSeek + 智谱GLM 双模型策略
 */
@Configuration
public class AiModelConfig {

    // ===== DeepSeek（默认主模型）=====
    @Value("${spring.ai.openai.api-key}")
    private String deepSeekApiKey;

    @Value("${spring.ai.openai.base-url}")
    private String deepSeekBaseUrl;

    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String deepSeekModel;

    // ===== 智谱GLM（备用模型）=====
    @Value("${glm.api-key:}")
    private String glmApiKey;

    @Value("${glm.base-url:https://open.bigmodel.cn/api/paas/v4}")
    private String glmBaseUrl;

    @Value("${glm.model:glm-4}")
    private String glmModel;

    @Bean
    @Primary
    public ChatModel chatModel() {
        return createModel(deepSeekBaseUrl, deepSeekApiKey, deepSeekModel);
    }

    @Bean("glmChatModel")
    public ChatModel glmChatModel() {
        return createModel(glmBaseUrl, glmApiKey, glmModel);
    }

    private ChatModel createModel(String baseUrl, String apiKey, String model) {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();

        return new OpenAiChatModel(api, OpenAiChatOptions.builder()
                .model(model)
                .temperature(0.3)
                .build());
    }
}
