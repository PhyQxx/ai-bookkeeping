package com.aibookkeeping.service.budget;

import com.aibookkeeping.dto.BudgetRequest;
import com.aibookkeeping.entity.Bill;
import com.aibookkeeping.entity.Budget;
import com.aibookkeeping.entity.Category;
import com.aibookkeeping.exception.BusinessException;
import com.aibookkeeping.exception.ErrorCode;
import com.aibookkeeping.mapper.BillMapper;
import com.aibookkeeping.mapper.BudgetMapper;
import com.aibookkeeping.mapper.CategoryMapper;
import com.aibookkeeping.service.ledger.LedgerService;
import com.aibookkeeping.vo.BudgetUsageVO;
import com.aibookkeeping.vo.BudgetVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetMapper budgetMapper;
    private final BillMapper billMapper;
    private final CategoryMapper categoryMapper;
    private final LedgerService ledgerService;

    @Override
    public List<BudgetVO> listBudgets(Long userId, String month) {
        Long ledgerId = ledgerService.getCurrentLedgerId(userId);
        LambdaQueryWrapper<Budget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Budget::getUserId, userId)
               .eq(Budget::getLedgerId, ledgerId)
               .eq(Budget::getMonth, month)
               .orderByAsc(Budget::getCategoryId);

        Map<Long, String> categoryMap = getCategoryMap();

        return budgetMapper.selectList(wrapper).stream()
                .map(b -> convertToVO(b, categoryMap))
                .collect(Collectors.toList());
    }

    @Override
    public BudgetVO createOrUpdateBudget(BudgetRequest request, Long userId) {
        Long ledgerId = ledgerService.getCurrentLedgerId(userId);
        // 查找同账本、同月、同分类的预算（Upsert 逻辑）
        LambdaQueryWrapper<Budget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Budget::getUserId, userId)
               .eq(Budget::getLedgerId, ledgerId)
               .eq(Budget::getMonth, request.getMonth())
               .eq(request.getCategoryId() != null, Budget::getCategoryId, request.getCategoryId())
               .isNull(request.getCategoryId() == null, Budget::getCategoryId);

        Budget existing = budgetMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setAmount(request.getAmount());
            budgetMapper.updateById(existing);
            return convertToVO(existing, getCategoryMap());
        }

        Budget budget = new Budget();
        budget.setUserId(userId);
        budget.setLedgerId(ledgerId);
        budget.setCategoryId(request.getCategoryId());
        budget.setAmount(request.getAmount());
        budget.setMonth(request.getMonth());
        budgetMapper.insert(budget);
        return convertToVO(budget, getCategoryMap());
    }

    @Override
    public void deleteBudget(Long id, Long userId) {
        Long ledgerId = ledgerService.getCurrentLedgerId(userId);
        Budget budget = budgetMapper.selectById(id);
        if (budget == null) {
            throw new BusinessException(ErrorCode.BUDGET_NOT_FOUND);
        }
        if (!budget.getUserId().equals(userId) || !budget.getLedgerId().equals(ledgerId)) {
            throw new BusinessException(ErrorCode.BUDGET_NO_PERMISSION);
        }
        budgetMapper.deleteById(id);
    }

    @Override
    public List<BudgetUsageVO> getBudgetUsage(Long userId, String month) {
        Long ledgerId = ledgerService.getCurrentLedgerId(userId);
        // 获取该账本该月所有预算
        List<BudgetVO> budgets = listBudgets(userId, month);
        if (budgets.isEmpty()) {
            return List.of();
        }

        // 查询该账本该月所有支出
        YearMonth yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        LambdaQueryWrapper<Bill> billWrapper = new LambdaQueryWrapper<>();
        billWrapper.eq(Bill::getUserId, userId)
                   .eq(Bill::getLedgerId, ledgerId)
                   .eq(Bill::getType, 2) // 只统计支出
                   .ge(Bill::getBillDate, startDate)
                   .le(Bill::getBillDate, endDate);

        List<Bill> bills = billMapper.selectList(billWrapper);

        // 按分类汇总已用金额
        Map<Long, BigDecimal> spentByCategory = bills.stream()
                .filter(b -> b.getCategoryId() != null)
                .collect(Collectors.groupingBy(
                        Bill::getCategoryId,
                        Collectors.reducing(BigDecimal.ZERO, Bill::getAmount, BigDecimal::add)
                ));

        BigDecimal totalSpent = bills.stream()
                .map(Bill::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, String> categoryMap = getCategoryMap();

        return budgets.stream()
                .map(budget -> {
                    BigDecimal budgetAmount = budget.getAmount();
                    BigDecimal usedAmount;

                    if (budget.getCategoryId() == null) {
                        // 月度总预算
                        usedAmount = totalSpent;
                    } else {
                        // 分类预算
                        usedAmount = spentByCategory.getOrDefault(budget.getCategoryId(), BigDecimal.ZERO);
                    }

                    BigDecimal remaining = budgetAmount.subtract(usedAmount);
                    BigDecimal percent = budgetAmount.compareTo(BigDecimal.ZERO) > 0
                            ? usedAmount.divide(budgetAmount, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;

                    return BudgetUsageVO.builder()
                            .budgetId(budget.getId())
                            .categoryId(budget.getCategoryId())
                            .categoryName(budget.getCategoryId() == null ? "总预算" :
                                    categoryMap.getOrDefault(budget.getCategoryId(), "未分类"))
                            .budgetAmount(budgetAmount)
                            .usedAmount(usedAmount)
                            .remainingAmount(remaining)
                            .usagePercent(percent.setScale(1, RoundingMode.HALF_UP))
                            .overBudget(usedAmount.compareTo(budgetAmount) > 0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private BudgetVO convertToVO(Budget budget, Map<Long, String> categoryMap) {
        return BudgetVO.builder()
                .id(budget.getId())
                .categoryId(budget.getCategoryId())
                .categoryName(budget.getCategoryId() == null ? "总预算" :
                        categoryMap.getOrDefault(budget.getCategoryId(), "未分类"))
                .amount(budget.getAmount())
                .month(budget.getMonth())
                .createdAt(budget.getCreatedAt())
                .build();
    }

    private Map<Long, String> getCategoryMap() {
        return categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }
}
