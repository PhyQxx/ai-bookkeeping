package com.aibookkeeping.service.audit;

import com.aibookkeeping.entity.AuditLog;
import com.aibookkeeping.mapper.AuditLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    @Override
    public Page<AuditLog> listMyLogs(Long userId, int pageNum, int pageSize) {
        Page<AuditLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLog::getUserId, userId)
               .orderByDesc(AuditLog::getCreatedAt);
        
        return auditLogMapper.selectPage(page, wrapper);
    }
}
