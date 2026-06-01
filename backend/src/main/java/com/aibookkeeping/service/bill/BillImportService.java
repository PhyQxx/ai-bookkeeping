package com.aibookkeeping.service.bill;

import com.aibookkeeping.dto.BillRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BillImportService {
    /**
     * 导入支付宝账单
     */
    int importAlipayCsv(MultipartFile file, Long userId);

    /**
     * 导入微信支付账单
     */
    int importWechatCsv(MultipartFile file, Long userId);
}
