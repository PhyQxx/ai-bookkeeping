package com.aibookkeeping.service.notification;

import com.aibookkeeping.entity.Notification;
import com.aibookkeeping.mapper.NotificationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public void createNotification(Long userId, String title, String content, String type) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setContent(content);
        n.setType(type);
        n.setIsRead(0);
        n.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(n);
    }

    @Override
    public List<Notification> listUnread(Long userId) {
        return notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .orderByDesc(Notification::getCreatedAt));
    }

    @Override
    public void markAsRead(Long id, Long userId) {
        Notification n = notificationMapper.selectById(id);
        if (n != null && n.getUserId().equals(userId)) {
            n.setIsRead(1);
            notificationMapper.updateById(n);
        }
    }

    @Override
    public void markAllAsRead(Long userId) {
        Notification n = new Notification();
        n.setIsRead(1);
        notificationMapper.update(n, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
    }
}
