package com.aibookkeeping.service.stat;

import com.aibookkeeping.entity.Bill;
import com.aibookkeeping.entity.Category;
import com.aibookkeeping.mapper.BillMapper;
import com.aibookkeeping.mapper.CategoryMapper;
import com.aibookkeeping.service.ledger.LedgerService;
import com.aibookkeeping.vo.CategoryRatioVO;
import com.aibookkeeping.vo.DailyTrendVO;
import com.aibookkeeping.vo.MonthlyStatVO;
import com.aibookkeeping.vo.TrendVO;
import com.aibookkeeping.vo.YearlyStatVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {

    private final BillMapper billMapper;
    private final CategoryMapper categoryMapper;
    private final LedgerService ledgerService;

    @Override
    public MonthlyStatVO getMonthlyStat(Long userId, String month) {
        Long ledgerId = ledgerService.getCurrentLedgerId(userId);
        YearMonth yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bill::getUserId, userId)
               .eq(Bill::getLedgerId, ledgerId)
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
    public YearlyStatVO getYearlyStat(Long userId, String year) {
        Long ledgerId = ledgerService.getCurrentLedgerId(userId);
        LocalDate startDate = LocalDate.of(Integer.parseInt(year), 1, 1);
        LocalDate endDate = LocalDate.of(Integer.parseInt(year), 12, 31);

        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bill::getUserId, userId)
               .eq(Bill::getLedgerId, ledgerId)
               .ge(Bill::getBillDate, startDate)
               .le(Bill::getBillDate, endDate);

        List<Bill> bills = billMapper.selectList(wrapper);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        int billCount = bills.size();

        Map<String, MonthlyStatVO> monthlyMap = new TreeMap<>();
        for (int i = 1; i <= 12; i++) {
            String m = String.format("%s-%02d", year, i);
            monthlyMap.put(m, MonthlyStatVO.builder().month(m).totalIncome(BigDecimal.ZERO).totalExpense(BigDecimal.ZERO).balance(BigDecimal.ZERO).billCount(0).build());
        }

        Map<Long, BigDecimal> categoryAmountMap = new TreeMap<>();

        for (Bill bill : bills) {
            String m = bill.getBillDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            MonthlyStatVO mStat = monthlyMap.get(m);
            if (bill.getType() == 1) {
                totalIncome = totalIncome.add(bill.getAmount());
                mStat.setTotalIncome(mStat.getTotalIncome().add(bill.getAmount()));
            } else {
                totalExpense = totalExpense.add(bill.getAmount());
                mStat.setTotalExpense(mStat.getTotalExpense().add(bill.getAmount()));
                if (bill.getCategoryId() != null) {
                    categoryAmountMap.merge(bill.getCategoryId(), bill.getAmount(), BigDecimal::add);
                }
            }
            mStat.setBillCount(mStat.getBillCount() + 1);
            mStat.setBalance(mStat.getTotalIncome().subtract(mStat.getTotalExpense()));
        }

        Map<Long, String> categoryMap = getCategoryMap();
        BigDecimal finalTotalExpense = totalExpense;
        List<CategoryRatioVO> breakdown = categoryAmountMap.entrySet().stream()
                .map(entry -> CategoryRatioVO.builder()
                        .categoryId(entry.getKey())
                        .categoryName(categoryMap.getOrDefault(entry.getKey(), "其他"))
                        .amount(entry.getValue())
                        .ratio(finalTotalExpense.compareTo(BigDecimal.ZERO) > 0 ? entry.getValue().divide(finalTotalExpense, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                        .build())
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .collect(Collectors.toList());

        return YearlyStatVO.builder()
                .year(year)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(totalIncome.subtract(totalExpense))
                .billCount(billCount)
                .monthlyDetails(new ArrayList<>(monthlyMap.values()))
                .categoryBreakdown(breakdown)
                .build();
    }

    @Override
    public List<CategoryRatioVO> getCategoryRatio(Long userId, String month, Integer type) {
        Long ledgerId = ledgerService.getCurrentLedgerId(userId);
        YearMonth yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bill::getUserId, userId)
               .eq(Bill::getLedgerId, ledgerId)
               .ge(Bill::getBillDate, startDate)
               .le(Bill::getBillDate, endDate);
        if (type != null) wrapper.eq(Bill::getType, type);

        List<Bill> bills = billMapper.selectList(wrapper);

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
    public List<TrendVO> getTrend(Long userId, String month, Integer months) {
        Long ledgerId = ledgerService.getCurrentLedgerId(userId);
        List<TrendVO> trends = new ArrayList<>();
        YearMonth end;

        if (month != null && !month.isEmpty()) {
            end = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
        } else {
            end = YearMonth.now();
        }

        for (int i = months - 1; i >= 0; i--) {
            YearMonth target = end.minusMonths(i);
            LocalDate startDate = target.atDay(1);
            LocalDate endDate = target.atEndOfMonth();

            LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Bill::getUserId, userId)
                   .eq(Bill::getLedgerId, ledgerId)
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

    @Override
    public List<DailyTrendVO> getDailyTrend(Long userId, String month) {
        Long ledgerId = ledgerService.getCurrentLedgerId(userId);
        YearMonth yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bill::getUserId, userId)
               .eq(Bill::getLedgerId, ledgerId)
               .ge(Bill::getBillDate, startDate)
               .le(Bill::getBillDate, endDate)
               .orderByAsc(Bill::getBillDate);

        List<Bill> bills = billMapper.selectList(wrapper);

        // 按日期分组
        Map<LocalDate, List<Bill>> billsByDate = bills.stream()
                .collect(Collectors.groupingBy(Bill::getBillDate));

        List<DailyTrendVO> result = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<Bill> dayBills = billsByDate.getOrDefault(date, List.of());
            BigDecimal income = dayBills.stream()
                    .filter(b -> b.getType() == 1)
                    .map(Bill::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal expense = dayBills.stream()
                    .filter(b -> b.getType() == 2)
                    .map(Bill::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            result.add(DailyTrendVO.builder()
                    .date(date.toString())
                    .income(income)
                    .expense(expense)
                    .build());
        }

        return result;
    }

    private Map<Long, String> getCategoryMap() {
        return categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }
}
