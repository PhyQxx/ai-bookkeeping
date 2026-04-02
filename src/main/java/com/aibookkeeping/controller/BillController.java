package com.aibookkeeping.controller;

import com.aibookkeeping.dto.AiParseRequest;
import com.aibookkeeping.dto.BillRequest;
import com.aibookkeeping.exception.Result;
import com.aibookkeeping.service.bill.BillService;
import com.aibookkeeping.vo.AiParseVO;
import com.aibookkeeping.vo.BillVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/list")
    @Operation(summary = "获取账单列表（分页+筛选）")
    public Result<Page<BillVO>> listBills(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Page<BillVO> page = billService.listBills(userId, type, categoryId, startDate, endDate, pageNum, pageSize);
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
