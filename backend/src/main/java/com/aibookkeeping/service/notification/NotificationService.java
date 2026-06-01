package com.aibookkeeping.service.notification;

import com.aibookkeeping.entity.Notification;
import java.util.List;

public interface NotificationService {
    void createNotification(Long userId, String title, String content, String type);
    List<Notification> listUnread(Long userId);
    void markAsRead(Long id, Long userId);
    void markAllAsRead(Long userId);
}
