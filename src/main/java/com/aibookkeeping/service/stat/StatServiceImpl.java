package com.aibookkeeping.service.stat;

import com.aibookkeeping.entity.Bill;
import com.aibookkeeping.entity.Category;
import com.aibookkeeping.mapper.BillMapper;
import com.aibookkeeping.mapper.CategoryMapper;
import com.aibookkeeping.vo.CategoryRatioVO;
import com.aibookkeeping.vo.MonthlyStatVO;
import com.aibookkeeping.vo.TrendVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {

    private final BillMapper billMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public MonthlyStatVO getMonthlyStat(Long userId, String month) {
        YearMonth yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bill::getUserId, userId)
               .ge(Bill::getBillDate, startDate)
               .le(Bill::getBillDate, endDate);

        List<Bill> bills = billMapper.selectList(wrapper);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        int billCount = bills.size();

        for (Bill bill : bills) {
            if (bill.getType() == 1) {
                totalIncome = totalIncome.add(bill.getAmount());
            } else {
                totalExpense = totalExpense.add(bill.getAmount());
            }
        }

        return MonthlyStatVO.builder()
                .month(month)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(totalIncome.subtract(totalExpense))
                .billCount(billCount)
                .build();
    }

    @Override
    public List<CategoryRatioVO> getCategoryRatio(Long userId, String month, Integer type) {
        YearMonth yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bill::getUserId, userId)
               .ge(Bill::getBillDate, startDate)
               .le(Bill::getBillDate, endDate);
        if (type != null) wrapper.eq(Bill::getType, type);

        List<Bill> bills = billMapper.selectList(wrapper);

        // 按 categoryId 分组汇总
        Map<Long, BigDecimal> amountByCategory = bills.stream()
                .filter(b -> b.getCategoryId() != null)
                .collect(Collectors.groupingBy(
                        Bill::getCategoryId,
                        Collectors.reducing(BigDecimal.ZERO, Bill::getAmount, BigDecimal::add)
                ));

        BigDecimal totalAmount = amountByCategory.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, String> categoryMap = getCategoryMap();

        return amountByCategory.entrySet().stream()
                .map(entry -> CategoryRatioVO.builder()
                        .categoryId(entry.getKey())
                        .categoryName(categoryMap.getOrDefault(entry.getKey(), "未分类"))
                        .amount(entry.getValue())
                        .ratio(totalAmount.compareTo(BigDecimal.ZERO) > 0
                                ? entry.getValue().divide(totalAmount, 4, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO)
                        .build())
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .collect(Collectors.toList());
    }

    @Override
    public List<TrendVO> getTrend(Long userId, Integer months) {
        List<TrendVO> trends = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = months - 1; i >= 0; i--) {
            YearMonth target = current.minusMonths(i);
            LocalDate startDate = target.atDay(1);
            LocalDate endDate = target.atEndOfMonth();

            LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Bill::getUserId, userId)
                   .ge(Bill::getBillDate, startDate)
                   .le(Bill::getBillDate, endDate);

            List<Bill> bills = billMapper.selectList(wrapper);

            BigDecimal income = bills.stream()
                    .filter(b -> b.getType() == 1)
                    .map(Bill::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal expense = bills.stream()
                    .filter(b -> b.getType() == 2)
                    .map(Bill::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            trends.add(TrendVO.builder()
                    .month(target.toString())
                    .income(income)
                    .expense(expense)
                    .build());
        }

        return trends;
    }

    private Map<Long, String> getCategoryMap() {
        return categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }
}
