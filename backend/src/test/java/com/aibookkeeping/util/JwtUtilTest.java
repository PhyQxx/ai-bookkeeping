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
        ReflectionTestUtils.setField(jwtUtil, "secret", "myTestSecretKeyForAES256EncryptionMustBe32Bytes!!");
    }

    @Test
    void testGenerateAndParseAccessToken() {
        String token = jwtUtil.generateAccessToken(1L, "testuser");

        assertNotNull(token);
        assertTrue(token.length() > 0);

        JwtUtil.TokenInfo info = jwtUtil.parseToken(token);
        assertNotNull(info);
        assertEquals(1L, info.userId());
        assertEquals("testuser", info.username());
        assertEquals("access", info.type());
    }

    @Test
    void testGenerateRefreshToken() {
        String token = jwtUtil.generateRefreshToken(1L, "testuser");

        JwtUtil.TokenInfo info = jwtUtil.parseToken(token);
        assertNotNull(info);
        assertEquals(1L, info.userId());
        assertEquals("testuser", info.username());
        assertEquals("refresh", info.type());
    }

    @Test
    void testGetUserIdAndUsername() {
        String token = jwtUtil.generateAccessToken(1L, "testuser");

        assertEquals(1L, jwtUtil.getUserId(token));
        assertEquals("testuser", jwtUtil.getUsername(token));
    }

    @Test
    void testValidateToken() {
        String token = jwtUtil.generateAccessToken(1L, "testuser");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void testInvalidToken() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
        assertFalse(jwtUtil.validateToken(""));
        assertFalse(jwtUtil.validateToken("//////"));
    }

    @Test
    void testIsRefreshToken() {
        String accessToken = jwtUtil.generateAccessToken(1L, "testuser");
        String refreshToken = jwtUtil.generateRefreshToken(1L, "testuser");

        assertFalse(jwtUtil.isRefreshToken(accessToken));
        assertTrue(jwtUtil.isRefreshToken(refreshToken));
    }

    @Test
    void testDifferentUsers() {
        String token1 = jwtUtil.generateAccessToken(1L, "user1");
        String token2 = jwtUtil.generateAccessToken(2L, "user2");

        assertEquals(1L, jwtUtil.getUserId(token1));
        assertEquals(2L, jwtUtil.getUserId(token2));
        assertNotEquals(token1, token2);
    }

    @Test
    void testSameUserSameContentDifferentToken() {
        // 每次加密使用随机 IV，所以相同数据每次生成的 token 不同
        String token1 = jwtUtil.generateAccessToken(1L, "testuser");
        String token2 = jwtUtil.generateAccessToken(1L, "testuser");

        assertNotEquals(token1, token2); // IV 不同导致密文不同
        assertEquals(jwtUtil.getUserId(token1), jwtUtil.getUserId(token2));
    }

    @Test
    void testTamperedToken() {
        String token = jwtUtil.generateAccessToken(1L, "testuser");
        String tampered = token + "x";
        assertFalse(jwtUtil.validateToken(tampered));
        assertNull(jwtUtil.getUserId(tampered));
    }
}
