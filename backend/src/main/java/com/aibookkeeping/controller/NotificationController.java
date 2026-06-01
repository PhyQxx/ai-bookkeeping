package com.aibookkeeping.controller;

import com.aibookkeeping.entity.Notification;
import com.aibookkeeping.exception.Result;
import com.aibookkeeping.service.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "消息通知")
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/unread")
    @Operation(summary = "获取未读消息列表")
    public Result<List<Notification>> listUnread(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(notificationService.listUnread(userId));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "标记单条消息为已读")
    public Result<Void> markAsRead(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        notificationService.markAsRead(id, userId);
        return Result.success();
    }

    @PostMapping("/read-all")
    @Operation(summary = "标记所有消息为已读")
    public Result<Void> markAllAsRead(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        notificationService.markAllAsRead(userId);
        return Result.success();
    }
}
