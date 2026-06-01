package com.aibookkeeping.controller;

import com.aibookkeeping.dto.LedgerRequest;
import com.aibookkeeping.exception.Result;
import com.aibookkeeping.service.ledger.LedgerService;
import com.aibookkeeping.vo.LedgerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "账本管理")
@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    @GetMapping("/list")
    @Operation(summary = "获取账本列表")
    public Result<List<LedgerVO>> listLedgers(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(ledgerService.listLedgers(userId));
    }

    @PostMapping
    @Operation(summary = "创建账本")
    public Result<LedgerVO> createLedger(@Valid @RequestBody LedgerRequest request,
                                         Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(ledgerService.createLedger(request, userId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新账本")
    public Result<LedgerVO> updateLedger(@PathVariable Long id,
                                         @Valid @RequestBody LedgerRequest request,
                                         Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(ledgerService.updateLedger(id, request, userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除账本")
    public Result<Void> deleteLedger(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        ledgerService.deleteLedger(id, userId);
        return Result.success();
    }

    @PostMapping("/switch/{id}")
    @Operation(summary = "切换当前账本")
    public Result<Void> switchLedger(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        ledgerService.switchLedger(id, userId);
        return Result.success();
    }
}
