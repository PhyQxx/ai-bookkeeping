package com.aibookkeeping.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Component
public class JwtUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.secret}")
    private String secret;

    private SecretKeySpec getKey() {
        byte[] keyBytes = new byte[32];
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(secretBytes, 0, keyBytes, 0, Math.min(secretBytes.length, 32));
        return new SecretKeySpec(keyBytes, "AES");
    }

    private String encrypt(TokenInfo info) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getKey(), new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] jsonBytes = objectMapper.writeValueAsBytes(info);
            byte[] cipherBytes = cipher.doFinal(jsonBytes);
            byte[] combined = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Token 加密失败", e);
        }
    }

    private TokenInfo decrypt(String token) {
        try {
            byte[] combined = Base64.getUrlDecoder().decode(token);
            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherBytes = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, cipherBytes, 0, cipherBytes.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] jsonBytes = cipher.doFinal(cipherBytes);
            return objectMapper.readValue(jsonBytes, TokenInfo.class);
        } catch (Exception e) {
            log.debug("Token 解密失败: {}", e.getMessage());
            return null;
        }
    }

    public String generateAccessToken(Long userId, String username) {
        return encrypt(new TokenInfo(userId, username, "access"));
    }

    public String generateRefreshToken(Long userId, String username) {
        return encrypt(new TokenInfo(userId, username, "refresh"));
    }

    /**
     * @deprecated 使用 generateAccessToken 代替
     */
    public String generateToken(Long userId, String username) {
        return generateAccessToken(userId, username);
    }

    public TokenInfo parseToken(String token) {
        return decrypt(token);
    }

    public Long getUserId(String token) {
        TokenInfo info = decrypt(token);
        return info != null ? info.userId : null;
    }

    public String getUsername(String token) {
        TokenInfo info = decrypt(token);
        return info != null ? info.username : null;
    }

    public String getTokenType(String token) {
        TokenInfo info = decrypt(token);
        return info != null ? info.type : null;
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenType(token));
    }

    public boolean validateToken(String token) {
        return decrypt(token) != null;
    }

    public record TokenInfo(Long userId, String username, String type) {
    }
}
