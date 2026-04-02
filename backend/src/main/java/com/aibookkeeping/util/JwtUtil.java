package com.aibookkeeping.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration:7200000}")
    private long accessExpiration; // 默认 2 小时

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration; // 默认 7 天

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Access Token（2小时）
     */
    public String generateAccessToken(Long userId, String username) {
        return Jwts.builder()
                .claims(Map.of("userId", userId, "username", username, "type", "access"))
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(getKey())
                .compact();
    }

    /**
     * 生成 Refresh Token（7天）
     */
    public String generateRefreshToken(Long userId, String username) {
        return Jwts.builder()
                .claims(Map.of("userId", userId, "username", username, "type", "refresh"))
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getKey())
                .compact();
    }

    /**
     * @deprecated 使用 generateAccessToken 代替
     */
    public String generateToken(Long userId, String username) {
        return generateAccessToken(userId, username);
    }

    /**
     * 解析 Token
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中获取用户ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从 Token 中获取用户名
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 获取 Token 类型（access / refresh）
     */
    public String getTokenType(String token) {
        Claims claims = parseToken(token);
        return claims.get("type", String.class);
    }

    /**
     * 判断是否为 Refresh Token
     */
    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(getTokenType(token));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
