package com.aibookkeeping.service.auth;

import com.aibookkeeping.dto.LoginRequest;
import com.aibookkeeping.dto.RegisterRequest;
import com.aibookkeeping.entity.LoginRecord;
import com.aibookkeeping.entity.User;
import com.aibookkeeping.exception.BusinessException;
import com.aibookkeeping.exception.ErrorCode;
import com.aibookkeeping.mapper.LoginRecordMapper;
import com.aibookkeeping.mapper.UserMapper;
import com.aibookkeeping.service.ledger.LedgerService;
import com.aibookkeeping.service.notification.NotificationService;
import com.aibookkeeping.util.JwtUtil;
import com.aibookkeeping.vo.LoginVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final LoginRecordMapper loginRecordMapper;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LedgerService ledgerService;

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
        
        // 为新用户初始化默认账本
        ledgerService.getCurrentLedgerId(user.getId());

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

        // 记录登录并检查异常
        handleLoginSecurity(user);

        // 确保用户有当前账本 ID
        Long ledgerId = ledgerService.getCurrentLedgerId(user.getId());

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        return LoginVO.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .currentLedgerId(ledgerId)
                .build();
    }

    private void handleLoginSecurity(User user) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return;
        
        HttpServletRequest request = attrs.getRequest();
        String currentIp = getClientIp(request);
        String ua = request.getHeader("User-Agent");

        // 查找最近登录的 IP
        List<LoginRecord> recentLogs = loginRecordMapper.selectList(new LambdaQueryWrapper<LoginRecord>()
                .eq(LoginRecord::getUserId, user.getId())
                .orderByDesc(LoginRecord::getLoginTime)
                .limit(5));
        
        boolean isNewIp = recentLogs.stream().noneMatch(log -> currentIp.equals(log.getIp()));

        // 保存登录记录
        LoginRecord record = new LoginRecord();
        record.setUserId(user.getId());
        record.setUsername(user.getUsername());
        record.setIp(currentIp);
        record.setUserAgent(ua);
        record.setLoginTime(LocalDateTime.now());
        loginRecordMapper.insert(record);

        if (isNewIp && !recentLogs.isEmpty()) {
            notificationService.createNotification(user.getId(), "🛡️ 异地登录提醒", 
                String.format("系统检测到您的账号在新的位置登录。登录IP: %s, 登录时间: %s。如果不是本人操作，请及时修改密码。", 
                currentIp, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))), "SECURITY");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "无效的刷新令牌");
        }

        Long userId = jwtUtil.getUserId(refreshToken);
        if (userId == null) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "令牌解密失败");
        }

        // 校验用户是否存在（激活逻辑）
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在，令牌已失效");
        }

        // 重新签发 Token
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        log.info("Token refreshed for user: {}", user.getUsername());

        return LoginVO.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .username(user.getUsername())
                .build();
    }

    @Override
    public void logout(String token) {
        // 客户端自行清除 Token，服务端无需操作
        try {
            Long userId = jwtUtil.getUserId(token);
            log.info("User logged out: userId={}", userId);
        } catch (Exception e) {
            log.warn("Logout ignored: {}", e.getMessage());
        }
    }
}
