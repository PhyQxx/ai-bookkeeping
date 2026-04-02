package com.aibookkeeping.service.auth;

import com.aibookkeeping.dto.LoginRequest;
import com.aibookkeeping.dto.RegisterRequest;
import com.aibookkeeping.entity.User;
import com.aibookkeeping.exception.BusinessException;
import com.aibookkeeping.exception.ErrorCode;
import com.aibookkeeping.mapper.UserMapper;
import com.aibookkeeping.util.JwtUtil;
import com.aibookkeeping.vo.LoginVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void register(RegisterRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectOne(wrapper) != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getUsername());
        userMapper.insert(user);

        log.info("User registered: {}", request.getUsername());
    }

    @Override
    public LoginVO login(LoginRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 生成双 Token
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        // 存入 Redis
        redisTemplate.opsForValue().set("token:access:" + user.getId(), accessToken, 2, TimeUnit.HOURS);
        redisTemplate.opsForValue().set("token:refresh:" + user.getId(), refreshToken, 7, TimeUnit.DAYS);

        return LoginVO.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .build();
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        // 验证 Refresh Token
        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "无效的刷新令牌");
        }
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "刷新令牌已过期，请重新登录");
        }

        // 检查黑名单
        if (Boolean.TRUE.equals(redisTemplate.hasKey("token:blacklist:" + refreshToken))) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "令牌已失效");
        }

        Long userId = jwtUtil.getUserId(refreshToken);
        String username = jwtUtil.getUsername(refreshToken);

        // 检查 Redis 中的 Refresh Token 是否匹配
        String storedRefreshToken = (String) redisTemplate.opsForValue().get("token:refresh:" + userId);
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "刷新令牌不匹配");
        }

        // 生成新的双 Token
        String newAccessToken = jwtUtil.generateAccessToken(userId, username);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, username);

        // 更新 Redis
        redisTemplate.opsForValue().set("token:access:" + userId, newAccessToken, 2, TimeUnit.HOURS);
        redisTemplate.opsForValue().set("token:refresh:" + userId, newRefreshToken, 7, TimeUnit.DAYS);

        // 旧 Refresh Token 加入黑名单
        redisTemplate.opsForValue().set("token:blacklist:" + refreshToken, "1", 7, TimeUnit.DAYS);

        log.info("Token refreshed for user: {}", username);

        return LoginVO.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .username(username)
                .build();
    }

    @Override
    public void logout(String token) {
        try {
            Long userId = jwtUtil.getUserId(token);
            // 将当前 Token 加入黑名单
            long remaining = jwtUtil.parseToken(token).getExpiration().getTime() - System.currentTimeMillis();
            if (remaining > 0) {
                redisTemplate.opsForValue().set("token:blacklist:" + token, "1", remaining, TimeUnit.MILLISECONDS);
            }
            // 清除 Redis 中的 Token
            redisTemplate.delete("token:access:" + userId);
            redisTemplate.delete("token:refresh:" + userId);
            log.info("User logged out: userId={}", userId);
        } catch (Exception e) {
            log.warn("Failed to logout: {}", e.getMessage());
        }
    }
}
