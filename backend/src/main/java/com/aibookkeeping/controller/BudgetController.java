package com.aibookkeeping.controller;

import com.aibookkeeping.dto.BudgetRequest;
import com.aibookkeeping.exception.Result;
import com.aibookkeeping.service.budget.BudgetService;
import com.aibookkeeping.vo.BudgetUsageVO;
import com.aibookkeeping.vo.BudgetVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budget")
@Tag(name = "预算管理", description = "预算设定、查询、超支提醒")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping("/list")
    @Operation(summary = "获取预算列表")
    public Result<List<BudgetVO>> listBudgets(
            @RequestParam String month,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<BudgetVO> list = budgetService.listBudgets(userId, month);
        return Result.success(list);
    }

    @PostMapping
    @Operation(summary = "创建或更新预算（Upsert）")
    public Result<BudgetVO> createOrUpdateBudget(@Valid @RequestBody BudgetRequest request,
                                                   Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        BudgetVO vo = budgetService.createOrUpdateBudget(request, userId);
        return Result.success(vo);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除预算")
    public Result<Void> deleteBudget(@PathVariable Long id,
                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        budgetService.deleteBudget(id, userId);
        return Result.success();
    }

    @GetMapping("/usage")
    @Operation(summary = "获取预算使用情况")
    public Result<List<BudgetUsageVO>> getBudgetUsage(
            @RequestParam String month,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<BudgetUsageVO> list = budgetService.getBudgetUsage(userId, month);
        return Result.success(list);
    }
}
