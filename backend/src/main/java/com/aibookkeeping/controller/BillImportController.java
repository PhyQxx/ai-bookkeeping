package com.aibookkeeping.controller;

import com.aibookkeeping.exception.Result;
import com.aibookkeeping.service.bill.BillImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "账单导入接口")
@RestController
@RequestMapping("/api/bill/import")
@RequiredArgsConstructor
public class BillImportController {

    private final BillImportService billImportService;

    @PostMapping("/alipay")
    @Operation(summary = "导入支付宝 CSV 账单")
    public Result<Integer> importAlipay(@RequestParam("file") MultipartFile file, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        int count = billImportService.importAlipayCsv(file, userId);
        return Result.success(count);
    }

    @PostMapping("/wechat")
    @Operation(summary = "导入微信支付 CSV 账单")
    public Result<Integer> importWechat(@RequestParam("file") MultipartFile file, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        int count = billImportService.importWechatCsv(file, userId);
        return Result.success(count);
    }
}
