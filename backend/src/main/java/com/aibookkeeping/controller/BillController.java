package com.aibookkeeping.controller;

import com.aibookkeeping.dto.AiConfirmRequest;
import com.aibookkeeping.dto.AiOcrRequest;
import com.aibookkeeping.dto.AiParseRequest;
import com.aibookkeeping.dto.BillRequest;
import com.aibookkeeping.exception.Result;
import com.aibookkeeping.service.bill.BillService;
import com.aibookkeeping.vo.AiParsePreviewVO;
import com.aibookkeeping.vo.AiParseVO;
import com.aibookkeeping.vo.BillVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bill")
@Tag(name = "账单管理", description = "AI 记账、手动记账、账单列表")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    @PostMapping("/ai-parse")
    @Operation(summary = "AI 解析自然语言并记账")
    public Result<AiParseVO> aiParse(@Valid @RequestBody AiParseRequest request,
                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AiParseVO result = billService.aiParseAndCreate(request, userId);
        return Result.success(result);
    }

    @PostMapping("/ai-parse-preview")
    @Operation(summary = "AI 解析预览（不入库）")
    public Result<AiParsePreviewVO> aiParsePreview(@Valid @RequestBody AiParseRequest request,
                                                    Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AiParsePreviewVO result = billService.aiParsePreview(request, userId);
        return Result.success(result);
    }

    @PostMapping("/ai-batch-parse-preview")
    @Operation(summary = "AI 批量解析预览（不入库）")
    public Result<List<AiParsePreviewVO>> aiBatchParsePreview(@Valid @RequestBody AiParseRequest request,
                                                               Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<AiParsePreviewVO> results = billService.aiBatchParsePreview(request, userId);
        return Result.success(results);
    }

    @PostMapping("/ai-ocr-preview")
    @Operation(summary = "AI 图片识别预览（不入库）")
    public Result<AiParsePreviewVO> aiOcrPreview(@Valid @RequestBody AiOcrRequest request,
                                                  Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AiParsePreviewVO result = billService.aiOcrPreview(request, userId);
        return Result.success(result);
    }

    @PostMapping("/ai-confirm")
    @Operation(summary = "确认 AI 解析结果并创建账单")
    public Result<BillVO> aiConfirm(@Valid @RequestBody AiConfirmRequest request,
                                     Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        BillVO vo = billService.aiConfirmCreate(request, userId);
        return Result.success(vo);
    }

    @PostMapping("/ai-batch-confirm")
    @Operation(summary = "批量确认 AI 解析结果并创建账单")
    public Result<List<BillVO>> aiBatchConfirm(@Valid @RequestBody List<AiConfirmRequest> requests,
                                                Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<BillVO> vos = billService.aiBatchConfirmCreate(requests, userId);
        return Result.success(vos);
    }

    @PostMapping
    @Operation(summary = "手动创建账单")
    public Result<BillVO> createBill(@Valid @RequestBody BillRequest request,
                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        BillVO vo = billService.createBill(request, userId);
        return Result.success(vo);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新账单")
    public Result<BillVO> updateBill(@PathVariable Long id,
                                      @Valid @RequestBody BillRequest request,
                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        BillVO vo = billService.updateBill(id, request, userId);
        return Result.success(vo);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除账单")
    public Result<Void> deleteBill(@PathVariable Long id,
                                    Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        billService.deleteBill(id, userId);
        return Result.success();
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除账单")
    public Result<Void> batchDeleteBills(@RequestBody List<Long> ids,
                                          Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        billService.batchDeleteBills(ids, userId);
        return Result.success();
    }

    @GetMapping("/list")
    @Operation(summary = "获取账单列表（分页+筛选）")
    public Result<Page<BillVO>> listBills(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) java.math.BigDecimal minAmount,
            @RequestParam(required = false) java.math.BigDecimal maxAmount,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Page<BillVO> page = billService.listBills(userId, type, categoryId, startDate, endDate, searchText, minAmount, maxAmount, pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取账单详情")
    public Result<BillVO> getBill(@PathVariable Long id,
                                   Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        BillVO vo = billService.getBill(id, userId);
        return Result.success(vo);
    }
}
