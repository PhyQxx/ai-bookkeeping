package com.aibookkeeping.controller;

import com.aibookkeeping.dto.RecurringBillRequest;
import com.aibookkeeping.exception.Result;
import com.aibookkeeping.service.bill.RecurringBillService;
import com.aibookkeeping.vo.RecurringBillVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "周期账单管理")
@RestController
@RequestMapping("/api/recurring")
@RequiredArgsConstructor
public class RecurringBillController {

    private final RecurringBillService recurringBillService;

    @GetMapping("/list")
    @Operation(summary = "获取周期账单规则列表")
    public Result<List<RecurringBillVO>> listRecurringBills(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(recurringBillService.listRecurringBills(userId));
    }

    @PostMapping
    @Operation(summary = "创建周期账单规则")
    public Result<RecurringBillVO> createRecurringBill(@Valid @RequestBody RecurringBillRequest request,
                                                       Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(recurringBillService.createRecurringBill(request, userId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新周期账单规则")
    public Result<RecurringBillVO> updateRecurringBill(@PathVariable Long id,
                                                       @Valid @RequestBody RecurringBillRequest request,
                                                       Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(recurringBillService.updateRecurringBill(id, request, userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除周期账单规则")
    public Result<Void> deleteRecurringBill(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        recurringBillService.deleteRecurringBill(id, userId);
        return Result.success();
    }
}
