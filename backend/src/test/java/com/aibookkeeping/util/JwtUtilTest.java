package com.aibookkeeping.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "myTestSecretKeyForAIBookkeepingSystem2024MustBeLongEnoughForHS256");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 604800000L);
    }

    @Test
    void testGenerateAndParseToken() {
        String token = jwtUtil.generateToken(1L, "testuser");

        assertNotNull(token);
        assertTrue(token.length() > 0);

        assertEquals(1L, jwtUtil.getUserId(token));
        assertEquals("testuser", jwtUtil.getUsername(token));
    }

    @Test
    void testValidateToken() {
        String token = jwtUtil.generateToken(1L, "testuser");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void testInvalidToken() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }

    @Test
    void testDifferentUsers() {
        String token1 = jwtUtil.generateToken(1L, "user1");
        String token2 = jwtUtil.generateToken(2L, "user2");

        assertEquals(1L, jwtUtil.getUserId(token1));
        assertEquals(2L, jwtUtil.getUserId(token2));
        assertNotEquals(token1, token2);
    }
}
