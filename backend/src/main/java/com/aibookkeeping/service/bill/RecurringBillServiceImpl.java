package com.aibookkeeping.service.bill;

import com.aibookkeeping.dto.RecurringBillRequest;
import com.aibookkeeping.entity.Bill;
import com.aibookkeeping.entity.Category;
import com.aibookkeeping.entity.RecurringBill;
import com.aibookkeeping.exception.BusinessException;
import com.aibookkeeping.exception.ErrorCode;
import com.aibookkeeping.mapper.BillMapper;
import com.aibookkeeping.mapper.CategoryMapper;
import com.aibookkeeping.mapper.RecurringBillMapper;
import com.aibookkeeping.service.ledger.LedgerService;
import com.aibookkeeping.vo.RecurringBillVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringBillServiceImpl implements RecurringBillService {

    private final RecurringBillMapper recurringBillMapper;
    private final BillMapper billMapper;
    private final CategoryMapper categoryMapper;
    private final LedgerService ledgerService;

    @Override
    public List<RecurringBillVO> listRecurringBills(Long userId) {
        Long ledgerId = ledgerService.getCurrentLedgerId(userId);
        LambdaQueryWrapper<RecurringBill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecurringBill::getUserId, userId)
               .eq(RecurringBill::getLedgerId, ledgerId)
               .orderByDesc(RecurringBill::getCreatedAt);
        
        List<RecurringBill> list = recurringBillMapper.selectList(wrapper);
        Map<Long, String> categoryMap = getCategoryMap();

        return list.stream().map(rb -> convertToVO(rb, categoryMap)).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecurringBillVO createRecurringBill(RecurringBillRequest request, Long userId) {
        Long ledgerId = ledgerService.getCurrentLedgerId(userId);
        RecurringBill rb = new RecurringBill();
        rb.setUserId(userId);
        rb.setLedgerId(ledgerId);
        rb.setAmount(request.getAmount());
        rb.setType(request.getType());
        rb.setCategoryId(request.getCategoryId());
        rb.setRemark(request.getRemark());
        rb.setFrequency(request.getFrequency());
        rb.setDayOfPeriod(request.getDayOfPeriod());
        rb.setStartDate(request.getStartDate());
        rb.setEndDate(request.getEndDate());
        rb.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        
        recurringBillMapper.insert(rb);
        return convertToVO(rb, getCategoryMap());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecurringBillVO updateRecurringBill(Long id, RecurringBillRequest request, Long userId) {
        Long ledgerId = ledgerService.getCurrentLedgerId(userId);
        RecurringBill rb = recurringBillMapper.selectById(id);
        if (rb == null || !rb.getUserId().equals(userId) || !rb.getLedgerId().equals(ledgerId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        rb.setAmount(request.getAmount());
        rb.setType(request.getType());
        rb.setCategoryId(request.getCategoryId());
        rb.setRemark(request.getRemark());
        rb.setFrequency(request.getFrequency());
        rb.setDayOfPeriod(request.getDayOfPeriod());
        rb.setStartDate(request.getStartDate());
        rb.setEndDate(request.getEndDate());
        if (request.getStatus() != null) rb.setStatus(request.getStatus());

        recurringBillMapper.updateById(rb);
        return convertToVO(rb, getCategoryMap());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecurringBill(Long id, Long userId) {
        Long ledgerId = ledgerService.getCurrentLedgerId(userId);
        RecurringBill rb = recurringBillMapper.selectById(id);
        if (rb == null || !rb.getUserId().equals(userId) || !rb.getLedgerId().equals(ledgerId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        recurringBillMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateBills() {
        LocalDate today = LocalDate.now();
        log.info("Starting recurring bill generation for {}", today);

        LambdaQueryWrapper<RecurringBill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecurringBill::getStatus, 1)
               .le(RecurringBill::getStartDate, today)
               .and(w -> w.isNull(RecurringBill::getEndDate).or().ge(RecurringBill::getEndDate, today));
        
        List<RecurringBill> activeRules = recurringBillMapper.selectList(wrapper);

        for (RecurringBill rule : activeRules) {
            if (shouldGenerateToday(rule, today)) {
                createBillFromRule(rule, today);
                rule.setLastGeneratedDate(today);
                recurringBillMapper.updateById(rule);
                log.info("Generated bill from rule ID {} for user {}", rule.getId(), rule.getUserId());
            }
        }
    }

    private boolean shouldGenerateToday(RecurringBill rule, LocalDate today) {
        // 如果今天已经生成过了，跳过
        if (rule.getLastGeneratedDate() != null && rule.getLastGeneratedDate().equals(today)) {
            return false;
        }

        String freq = rule.getFrequency();
        if ("DAILY".equals(freq)) return true;
        
        if ("WEEKLY".equals(freq)) {
            return today.getDayOfWeek().getValue() == rule.getDayOfPeriod();
        }

        if ("MONTHLY".equals(freq)) {
            int day = rule.getDayOfPeriod();
            int lastDayOfMonth = today.lengthOfMonth();
            // 如果设定的日期超过当月最大天数（如 31 号），则在最后一天执行
            if (day > lastDayOfMonth) {
                return today.getDayOfMonth() == lastDayOfMonth;
            }
            return today.getDayOfMonth() == day;
        }

        return false;
    }

    private void createBillFromRule(RecurringBill rule, LocalDate date) {
        Bill bill = new Bill();
        bill.setUserId(rule.getUserId());
        bill.setAmount(rule.getAmount());
        bill.setType(rule.getType());
        bill.setCategoryId(rule.getCategoryId());
        bill.setBillDate(date);
        bill.setRemark("[自动] " + (rule.getRemark() != null ? rule.getRemark() : ""));
        bill.setInputType(2); // 手动/系统
        bill.setCreatedAt(LocalDateTime.now());
        billMapper.insert(bill);
    }

    private Map<Long, String> getCategoryMap() {
        return categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }

    private RecurringBillVO convertToVO(RecurringBill rb, Map<Long, String> categoryMap) {
        return RecurringBillVO.builder()
                .id(rb.getId())
                .amount(rb.getAmount())
                .type(rb.getType())
                .typeName(rb.getType() == 1 ? "收入" : "支出")
                .categoryId(rb.getCategoryId())
                .categoryName(categoryMap.getOrDefault(rb.getCategoryId(), "未知"))
                .remark(rb.getRemark())
                .frequency(rb.getFrequency())
                .dayOfPeriod(rb.getDayOfPeriod())
                .startDate(rb.getStartDate())
                .endDate(rb.getEndDate())
                .lastGeneratedDate(rb.getLastGeneratedDate())
                .status(rb.getStatus())
                .createdAt(rb.getCreatedAt())
                .build();
    }
}
