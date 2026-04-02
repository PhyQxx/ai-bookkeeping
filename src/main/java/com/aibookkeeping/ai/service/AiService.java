package com.aibookkeeping.ai.service;

import com.aibookkeeping.ai.model.BillParseResult;
import com.aibookkeeping.ai.prompt.PromptTemplates;
import com.aibookkeeping.exception.BusinessException;
import com.aibookkeeping.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * AI 服务 - 支持主模型(DeepSeek) + 备用模型(GLM) 自动降级
 */
@Slf4j
@Service
public class AiService {

    private final ChatClient primaryClient;
    private final ChatClient fallbackClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.cache.expire-minutes:5}")
    private int cacheExpireMinutes;

    @Value("${ai.rate-limit.max-requests:20}")
    private int rateLimitMax;

    @Value("${ai.rate-limit.window-minutes:1}")
    private int rateLimitWindow;

    public AiService(ChatModel chatModel,
                     @Qualifier("glmChatModel") ChatModel glmChatModel,
                     RedisTemplate<String, Object> redisTemplate) {
        this.primaryClient = ChatClient.builder(chatModel).build();
        this.fallbackClient = ChatClient.builder(glmChatModel).build();
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * AI 解析自然语言记账输入（带缓存 + 限流 + 降级）
     */
    public BillParseResult parseBillInput(String input, Long userId) {
        // 限流检查
        checkRateLimit(userId);

        // 缓存检查
        String cacheKey = "ai:parse:" + input.hashCode();
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("AI parse cache hit for: {}", input);
            try {
                return objectMapper.convertValue(cached, BillParseResult.class);
            } catch (Exception e) {
                log.warn("Cache deserialize failed, calling AI");
            }
        }

        // 先尝试主模型(DeepSeek)
        BillParseResult result = callAi(input, primaryClient, "DeepSeek");

        // 主模型失败，降级到 GLM
        if (result == null) {
            log.warn("Primary model failed, falling back to GLM");
            result = callAi(input, fallbackClient, "GLM");
        }

        // 两个模型都失败
        if (result == null) {
            throw new BusinessException(ErrorCode.AI_PARSE_FAILED);
        }

        // 校验解析结果
        if (result.getAmount() == null || result.getAmount().doubleValue() <= 0) {
            throw new BusinessException(ErrorCode.AI_PARSE_AMOUNT_INVALID);
        }

        // 写入缓存
        redisTemplate.opsForValue().set(cacheKey, result, cacheExpireMinutes, TimeUnit.MINUTES);
        return result;
    }

    /**
     * AI 推荐消费分类
     */
    public String recommendCategory(String description, Long userId) {
        checkRateLimit(userId);

        String result = callSimpleAi(description, primaryClient);
        if (result == null) {
            result = callSimpleAi(description, fallbackClient);
        }
        return result != null ? result.trim() : "其他";
    }

    /**
     * 限流检查：每个用户每分钟最多调用 N 次 AI
     */
    private void checkRateLimit(Long userId) {
        String key = "ai:rate:" + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, rateLimitWindow, TimeUnit.MINUTES);
        }
        if (count != null && count > rateLimitMax) {
            throw new BusinessException(ErrorCode.AI_RATE_LIMITED);
        }
    }

    /**
     * 调用 AI 解析记账
     */
    private BillParseResult callAi(String input, ChatClient client, String modelName) {
        try {
            String response = client.prompt()
                    .system(PromptTemplates.BILL_PARSE_SYSTEM)
                    .user(input)
                    .call()
                    .content();

            log.info("[{}] AI response for '{}': {}", modelName, input, response);
            return parseResponse(response);
        } catch (Exception e) {
            log.error("[{}] AI parse error: {}", modelName, e.getMessage());
            return null;
        }
    }

    /**
     * 调用 AI 推荐分类
     */
    private String callSimpleAi(String input, ChatClient client) {
        try {
            return client.prompt()
                    .system(PromptTemplates.CATEGORY_RECOMMEND_SYSTEM)
                    .user(input)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI category recommend error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析 AI 返回的 JSON
     */
    private BillParseResult parseResponse(String response) {
        try {
            String json = response;
            if (response.contains("```")) {
                int start = response.indexOf("```") + 3;
                if (response.startsWith("```json")) start = response.indexOf("\n") + 1;
                int end = response.lastIndexOf("```");
                if (end > start) {
                    json = response.substring(start, end).trim();
                }
            }

            JsonNode node = objectMapper.readTree(json);
            BillParseResult result = new BillParseResult();

            if (node.has("amount")) result.setAmount(node.get("amount").decimalValue());
            if (node.has("category")) result.setCategory(node.get("category").asText());
            if (node.has("date")) {
                try {
                    result.setDate(LocalDate.parse(node.get("date").asText(), DateTimeFormatter.ISO_LOCAL_DATE));
                } catch (Exception e) {
                    result.setDate(LocalDate.now());
                }
            } else {
                result.setDate(LocalDate.now());
            }
            if (node.has("remark")) result.setRemark(node.get("remark").asText());

            return result;
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", response, e);
            return null;
        }
    }
}
