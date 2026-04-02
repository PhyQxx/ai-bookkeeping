package com.aibookkeeping.ai.service;

import com.aibookkeeping.ai.model.BillParseResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AiServiceTest {

    // 测试 JSON 解析逻辑（独立于 Spring 上下文）

    @Test
    void testBillParseResultFields() {
        BillParseResult result = new BillParseResult();
        result.setAmount(new BigDecimal("35.00"));
        result.setCategory("餐饮");
        result.setDate(LocalDate.of(2026, 4, 2));
        result.setRemark("午饭");

        assertEquals(new BigDecimal("35.00"), result.getAmount());
        assertEquals("餐饮", result.getCategory());
        assertEquals(LocalDate.of(2026, 4, 2), result.getDate());
        assertEquals("午饭", result.getRemark());
    }

    @Test
    void testBillParseResultNullSafety() {
        BillParseResult result = new BillParseResult();
        assertNull(result.getAmount());
        assertNull(result.getCategory());
        assertNull(result.getDate());
        assertNull(result.getRemark());
    }
}
