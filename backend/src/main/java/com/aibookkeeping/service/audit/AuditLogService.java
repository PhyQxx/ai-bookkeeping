package com.aibookkeeping.service.audit;

import com.aibookkeeping.entity.AuditLog;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface AuditLogService {
    Page<AuditLog> listMyLogs(Long userId, int pageNum, int pageSize);
}
