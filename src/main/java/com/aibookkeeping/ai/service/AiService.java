package com.aibookkeeping.ai.service;

import com.aibookkeeping.ai.model.BillParseResult;
import com.aibookkeeping.ai.prompt.PromptTemplates;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AiService {

    private final ChatClient chatClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.cache.expire-minutes:5}")
    private int cacheExpireMinutes;

    public AiService(@Qualifier("chatModel") ChatClient.Builder chatClientBuilder,
                     RedisTemplate<String, Object> redisTemplate) {
        this.chatClient = chatClientBuilder.build();
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * AI 解析自然语言记账输入
     * 带缓存：相同输入短时间内直接返回缓存结果
     */
    public BillParseResult parseBillInput(String input) {
        // 检查缓存
        String cacheKey = "ai:parse:" + input.hashCode();
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("AI parse cache hit for: {}", input);
            try {
                return objectMapper.convertValue(cached, BillParseResult.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize cached result, calling AI");
            }
        }

        try {
            String response = chatClient.prompt()
                    .system(PromptTemplates.BILL_PARSE_SYSTEM)
                    .user(input)
                    .call()
                    .content();

            log.info("AI response for '{}': {}", input, response);

            BillParseResult result = parseResponse(response);
            if (result != null) {
                // 写入缓存
                redisTemplate.opsForValue().set(cacheKey, result, cacheExpireMinutes, TimeUnit.MINUTES);
            }
            return result;
        } catch (Exception e) {
            log.error("AI parse failed for input: {}", input, e);
            return null;
        }
    }

    /**
     * AI 推荐消费分类
     */
    public String recommendCategory(String description) {
        try {
            String response = chatClient.prompt()
                    .system(PromptTemplates.CATEGORY_RECOMMEND_SYSTEM)
                    .user(description)
                    .call()
                    .content();
            return response.trim();
        } catch (Exception e) {
            log.error("AI category recommend failed: {}", e.getMessage());
            return "其他";
        }
    }

    /**
     * 解析 AI 返回的 JSON 字符串为 BillParseResult
     */
    private BillParseResult parseResponse(String response) {
        try {
            // 清理响应内容，提取 JSON 部分
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

            if (node.has("amount")) {
                result.setAmount(node.get("amount").decimalValue());
            }
            if (node.has("category")) {
                result.setCategory(node.get("category").asText());
            }
            if (node.has("date")) {
                try {
                    result.setDate(LocalDate.parse(node.get("date").asText(), DateTimeFormatter.ISO_LOCAL_DATE));
                } catch (Exception e) {
                    result.setDate(LocalDate.now());
                }
            } else {
                result.setDate(LocalDate.now());
            }
            if (node.has("remark")) {
                result.setRemark(node.get("remark").asText());
            }

            return result;
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", response, e);
            return null;
        }
    }
}
