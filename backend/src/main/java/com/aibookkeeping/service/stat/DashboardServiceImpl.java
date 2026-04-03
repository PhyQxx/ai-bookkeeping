package com.aibookkeeping.service.stat;

import com.aibookkeeping.entity.Bill;
import com.aibookkeeping.entity.Budget;
import com.aibookkeeping.entity.Category;
import com.aibookkeeping.mapper.BillMapper;
import com.aibookkeeping.mapper.BudgetMapper;
import com.aibookkeeping.mapper.CategoryMapper;
import com.aibookkeeping.vo.BudgetProgressVO;
import com.aibookkeeping.vo.CategoryRankingVO;
import com.aibookkeeping.vo.DailyTrendVO;
import com.aibookkeeping.vo.MonthlyStatVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BillMapper billMapper;
    private final BudgetMapper budgetMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public MonthlyStatVO getMonthlySummary(Long userId) {
        YearMonth now = YearMonth.now();
        LocalDate startDate = now.atDay(1);
        LocalDate endDate = now.atEndOfMonth();

        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bill::getUserId, userId)
               .ge(Bill::getBillDate, startDate)
               .le(Bill::getBillDate, endDate);

        List<Bill> bills = billMapper.selectList(wrapper);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (Bill bill : bills) {
            if (bill.getType() == 1) totalIncome = totalIncome.add(bill.getAmount());
            else totalExpense = totalExpense.add(bill.getAmount());
        }

        return MonthlyStatVO.builder()
                .month(now.toString())
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(totalIncome.subtract(totalExpense))
                .billCount(bills.size())
                .build();
    }

    @Override
    public List<DailyTrendVO> getRecentTrend(Long userId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(29);

        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bill::getUserId, userId)
               .ge(Bill::getBillDate, startDate)
               .le(Bill::getBillDate, endDate)
               .orderByAsc(Bill::getBillDate);

        List<Bill> bills = billMapper.selectList(wrapper);
        Map<LocalDate, List<Bill>> byDate = bills.stream()
                .collect(Collectors.groupingBy(Bill::getBillDate));

        List<DailyTrendVO> result = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            List<Bill> dayBills = byDate.getOrDefault(d, List.of());
            BigDecimal income = dayBills.stream().filter(b -> b.getType() == 1)
                    .map(Bill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal expense = dayBills.stream().filter(b -> b.getType() == 2)
                    .map(Bill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(DailyTrendVO.builder()
                    .date(d.toString()).income(income).expense(expense).build());
        }
        return result;
    }

    @Override
    public List<CategoryRankingVO> getCategoryRanking(Long userId) {
        YearMonth now = YearMonth.now();
        LocalDate startDate = now.atDay(1);
        LocalDate endDate = now.atEndOfMonth();

        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bill::getUserId, userId)
               .eq(Bill::getType, 2) // expense only
               .ge(Bill::getBillDate, startDate)
               .le(Bill::getBillDate, endDate);

        List<Bill> bills = billMapper.selectList(wrapper);
        Map<Long, String> categoryMap = getCategoryMap();

        // Group by category
        Map<Long, List<Bill>> byCategory = bills.stream()
                .filter(b -> b.getCategoryId() != null)
                .collect(Collectors.groupingBy(Bill::getCategoryId));

        BigDecimal totalExpense = bills.stream()
                .map(Bill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return byCategory.entrySet().stream()
                .map(e -> {
                    BigDecimal amount = e.getValue().stream()
                            .map(Bill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal pct = totalExpense.compareTo(BigDecimal.ZERO) > 0
                            ? amount.divide(totalExpense, 4, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return CategoryRankingVO.builder()
                            .categoryId(e.getKey())
                            .categoryName(categoryMap.getOrDefault(e.getKey(), "未分类"))
                            .amount(amount)
                            .count(e.getValue().size())
                            .percentage(pct)
                            .build();
                })
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .limit(5)
                .collect(Collectors.toList());
    }

    @Override
    public List<BudgetProgressVO> getBudgetProgress(Long userId) {
        String month = YearMonth.now().toString();
        Map<Long, String> categoryMap = getCategoryMap();

        LambdaQueryWrapper<Budget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Budget::getUserId, userId).eq(Budget::getMonth, month);
        List<Budget> budgets = budgetMapper.selectList(wrapper);

        if (budgets.isEmpty()) return List.of();

        LocalDate startDate = YearMonth.now().atDay(1);
        LocalDate endDate = YearMonth.now().atEndOfMonth();

        // Calculate used amount per category
        LambdaQueryWrapper<Bill> billWrapper = new LambdaQueryWrapper<>();
        billWrapper.eq(Bill::getUserId, userId)
                   .eq(Bill::getType, 2)
                   .ge(Bill::getBillDate, startDate)
                   .le(Bill::getBillDate, endDate);
        List<Bill> bills = billMapper.selectList(billWrapper);

        Map<Long, BigDecimal> usedByCategory = bills.stream()
                .filter(b -> b.getCategoryId() != null)
                .collect(Collectors.groupingBy(
                        Bill::getCategoryId,
                        Collectors.reducing(BigDecimal.ZERO, Bill::getAmount, BigDecimal::add)
                ));

        // Total used for null category budgets
        BigDecimal totalUsed = bills.stream()
                .map(Bill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return budgets.stream().map(b -> {
            BigDecimal used = b.getCategoryId() != null
                    ? usedByCategory.getOrDefault(b.getCategoryId(), BigDecimal.ZERO)
                    : totalUsed;
            BigDecimal pct = b.getAmount().compareTo(BigDecimal.ZERO) > 0
                    ? used.divide(b.getAmount(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;
            return BudgetProgressVO.builder()
                    .budgetId(b.getId())
                    .categoryName(b.getCategoryId() != null
                            ? categoryMap.getOrDefault(b.getCategoryId(), "未分类")
                            : "总预算")
                    .budgetAmount(b.getAmount())
                    .usedAmount(used)
                    .remainingAmount(b.getAmount().subtract(used))
                    .usagePercent(pct)
                    .overBudget(used.compareTo(b.getAmount()) > 0)
                    .build();
        }).collect(Collectors.toList());
    }

    private Map<Long, String> getCategoryMap() {
        return categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }
}
