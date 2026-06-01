package com.aibookkeeping.ai.service;

import com.aibookkeeping.ai.model.BillParseResult;
import com.aibookkeeping.ai.prompt.PromptTemplates;
import com.aibookkeeping.exception.BusinessException;
import com.aibookkeeping.vo.AiParsePreviewVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.Media;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AI 服务 - 支持自然语言解析、批量解析及图片 OCR 识别
 */
@Slf4j
@Service
public class AiService {

    private final ChatClient primaryClient;
    private final ChatClient fallbackClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final BillMapper billMapper;
    private final LedgerService ledgerService;
    private final ObjectMapper objectMapper;

    @Value("${ai.cache.expire-minutes:5}")
    private int cacheExpireMinutes;

    @Value("${ai.rate-limit.max-requests:20}")
    private int rateLimitMax;

    @Value("${ai.rate-limit.window-minutes:1}")
    private int rateLimitWindow;

    @Value("${ai.vision-model:glm-4v}")
    private String visionModel;

    public AiService(ChatModel chatModel,
                     @Qualifier("glmChatModel") ChatModel glmChatModel,
                     RedisTemplate<String, Object> redisTemplate,
                     BillMapper billMapper,
                     LedgerService ledgerService) {
        this.primaryClient = ChatClient.builder(chatModel).build();
        this.fallbackClient = ChatClient.builder(glmChatModel).build();
        this.redisTemplate = redisTemplate;
        this.billMapper = billMapper;
        this.ledgerService = ledgerService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * AI 解析小票图片 (OCR)
     */
    public BillParseResult parseBillImage(String base64Image, Long userId) {
        checkRateLimit(userId);
        
        // 提取真正的 Base64 内容
        String pureBase64 = base64Image;
        if (base64Image.contains(",")) {
            pureBase64 = base64Image.split(",")[1];
        }

        try {
            byte[] imageBytes = Base64.getDecoder().decode(pureBase64);
            Media media = new Media(MimeTypeUtils.IMAGE_JPEG, imageBytes);

            // 调用支持 Vision 的模型
            String response = fallbackClient.prompt()
                    .system(PromptTemplates.BILL_OCR_SYSTEM)
                    .user(u -> u.text("请识别这张小票").media(media))
                    .call()
                    .content();

            log.info("AI OCR response: {}", response);
            return parseResponse(response);
        } catch (Exception e) {
            log.error("AI OCR parse failed", e);
            throw new BusinessException(ErrorCode.AI_PARSE_FAILED, "图片解析失败: " + e.getMessage());
        }
    }

    /**
     * AI 解析自然语言记账输入（带缓存 + 限流 + 降级）
     */
    public BillParseResult parseBillInput(String input, Long userId) {
        // 限流检查
        checkRateLimit(userId);

        // 获取当前账本
        Long ledgerId = ledgerService.getCurrentLedgerId(userId);

        // 获取用户习惯上下文（基于当前账本）
        String userContext = getUserContext(userId, ledgerId);

        // 缓存检查 (包含 ledgerId 以实现隔离)
        String cacheKey = "ai:parse:" + userId + ":" + ledgerId + ":" + input.hashCode();
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
        BillParseResult result = callAi(input, userContext, primaryClient, "DeepSeek");

        // 主模型失败，降级到 GLM
        if (result == null) {
            log.warn("Primary model failed, falling back to GLM");
            result = callAi(input, userContext, fallbackClient, "GLM");
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
        Long ledgerId = ledgerService.getCurrentLedgerId(userId);
        String userContext = getUserContext(userId, ledgerId);

        String result = callSimpleAi(description, userContext, primaryClient);
        if (result == null) {
            result = callSimpleAi(description, userContext, fallbackClient);
        }
        return result != null ? result.trim() : "其他";
    }

    /**
     * AI 批量解析自然语言记账输入
     */
    public List<BillParseResult> parseBatchBillInput(String input, Long userId) {
        checkRateLimit(userId);
        Long ledgerId = ledgerService.getCurrentLedgerId(userId);
        String userContext = getUserContext(userId, ledgerId);

        String cacheKey = "ai:parse:batch:" + userId + ":" + ledgerId + ":" + input.hashCode();
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(objectMapper.writeValueAsString(cached),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, BillParseResult.class));
            } catch (Exception e) {
                log.warn("Batch cache deserialize failed");
            }
        }

        List<BillParseResult> results = callBatchAi(input, userContext, primaryClient, "DeepSeek");
        if (results == null || results.isEmpty()) {
            log.warn("Primary model failed for batch, falling back to GLM");
            results = callBatchAi(input, userContext, fallbackClient, "GLM");
        }

        if (results == null || results.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_PARSE_FAILED);
        }

        redisTemplate.opsForValue().set(cacheKey, results, cacheExpireMinutes, TimeUnit.MINUTES);
        return results;
    }

    /**
     * 获取用户习惯上下文
     */
    private String getUserContext(Long userId, Long ledgerId) {
        try {
            // 这里我们需要修改 BillMapper 的 selectFrequentCategoryMappings 来支持 ledgerId
            // 或者暂时通过 userId 共享习惯（根据之前的讨论，分类是共享的，但习惯可以按账本区分）
            // 为了更好的体验，我们假设 Mapper 已经支持或我们现在去支持它
            List<Map<String, Object>> mappings = billMapper.selectFrequentCategoryMappingsByLedger(userId, ledgerId);
            if (mappings.isEmpty()) return "";

            StringBuilder sb = new StringBuilder("\n## 用户记账习惯 (优先参考)\n");
            for (Map<String, Object> m : mappings) {
                sb.append("- ").append(m.get("remark")).append(" -> ").append(m.get("categoryName")).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Failed to get user context: {}", e.getMessage());
            return "";
        }
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
    private BillParseResult callAi(String input, String context, ChatClient client, String modelName) {
        try {
            String response = client.prompt()
                    .system(PromptTemplates.BILL_PARSE_SYSTEM + context)
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
    private String callSimpleAi(String input, String context, ChatClient client) {
        try {
            return client.prompt()
                    .system(PromptTemplates.CATEGORY_RECOMMEND_SYSTEM + context)
                    .user(input)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI category recommend error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 调用 AI 批量解析记账
     */
    private List<BillParseResult> callBatchAi(String input, String context, ChatClient client, String modelName) {
        try {
            String response = client.prompt()
                    .system(PromptTemplates.BATCH_BILL_PARSE_SYSTEM + context)
                    .user(input)
                    .call()
                    .content();

            log.info("[{}] AI batch response for '{}': {}", modelName, input, response);
            return parseBatchResponse(response);
        } catch (Exception e) {
            log.error("[{}] AI batch parse error: {}", modelName, e.getMessage());
            return null;
        }
    }

    /**
     * 解析 AI 返回的 JSON 数组
     */
    private List<BillParseResult> parseBatchResponse(String response) {
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

            JsonNode rootNode = objectMapper.readTree(json);
            List<BillParseResult> results = new ArrayList<>();

            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
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
                    results.add(result);
                }
            }
            return results;
        } catch (Exception e) {
            log.error("Failed to parse AI batch response: {}", response, e);
            return null;
        }
    }
}
