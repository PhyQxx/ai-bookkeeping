package com.aibookkeeping.service.bill;

import com.aibookkeeping.ai.model.BillParseResult;
import com.aibookkeeping.ai.service.AiService;
import com.aibookkeeping.dto.AiConfirmRequest;
import com.aibookkeeping.dto.AiOcrRequest;
import com.aibookkeeping.dto.AiParseRequest;
import com.aibookkeeping.dto.BillRequest;
import com.aibookkeeping.entity.Bill;
import com.aibookkeeping.entity.Budget;
import com.aibookkeeping.entity.Category;
import com.aibookkeeping.exception.BusinessException;
import com.aibookkeeping.exception.ErrorCode;
import com.aibookkeeping.mapper.BillMapper;
import com.aibookkeeping.mapper.BudgetMapper;
import com.aibookkeeping.mapper.CategoryMapper;
import com.aibookkeeping.service.ledger.LedgerService;
import com.aibookkeeping.service.notification.NotificationService;
import com.aibookkeeping.vo.AiParsePreviewVO;
import com.aibookkeeping.vo.AiParseVO;
import com.aibookkeeping.vo.BillVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillServiceImpl implements BillService {

    private final BillMapper billMapper;
    private final CategoryMapper categoryMapper;
    private final BudgetMapper budgetMapper;
    private final AiService aiService;
    private final NotificationService notificationService;
    private final LedgerService ledgerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiParseVO aiParseAndCreate(AiParseRequest request, Long userId) {
        Long ledgerId = ledgerService.getCurrentLedgerId(userId);
        BillParseResult result = aiService.parseBillInput(request.getInput(), userId);
        Long categoryId = findCategoryIdByName(result.getCategory());

        Bill bill = new Bill();
        bill.setUserId(userId);
        bill.setLedgerId(ledgerId);
        bill.setAmount(result.getAmount().abs());
        bill.setType(result.getAmount().compareTo(BigDecimal.ZERO) >= 0 ? 2 : 1);
        bill.setCategoryId(categoryId);
        bill.setBillDate(result.getDate() != null ? result.getDate() : LocalDate.now());
        bill.setRemark(result.getRemark());
        bill.setInputType(1);
        billMapper.insert(bill);

        checkBudget(userId, bill);

        return AiParseVO.builder()
                .success(true)
                .amount(bill.getAmount())
                .category(result.getCategory())
                .categoryId(categoryId)
                .date(bill.getBillDate().toString())
                .remark(result.getRemark())
                .billId(bill.getId())
                .build();
    }

    @Override
    public AiParsePreviewVO aiParsePreview(AiParseRequest request, Long userId) {
        BillParseResult result;
        try {
            result = aiService.parseBillInput(request.getInput(), userId);
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCode.AI_PARSE_PREVIEW_FAILED);
        }

        Long categoryId = findCategoryIdByName(result.getCategory());

        // 获取候选分类列表
        List<AiParsePreviewVO.CategoryCandidate> candidates = categoryMapper.selectList(null).stream()
                .map(c -> AiParsePreviewVO.CategoryCandidate.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .type(c.getType())
                        .build())
                .collect(Collectors.toList());

        return AiParsePreviewVO.builder()
                .amount(result.getAmount().abs())
                .category(result.getCategory())
                .categoryId(categoryId)
                .date(result.getDate() != null ? result.getDate().toString() : LocalDate.now().toString())
                .remark(result.getRemark())
                .candidates(candidates)
                .build();
    }

    @Override
    public List<AiParsePreviewVO> aiBatchParsePreview(AiParseRequest request, Long userId) {
        // ... (existing logic) ...
        List<BillParseResult> results;
        try {
            results = aiService.parseBatchBillInput(request.getInput(), userId);
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCode.AI_PARSE_PREVIEW_FAILED);
        }

        List<AiParsePreviewVO.CategoryCandidate> candidates = categoryMapper.selectList(null).stream()
                .map(c -> AiParsePreviewVO.CategoryCandidate.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .type(c.getType())
                        .build())
                .collect(Collectors.toList());

        return results.stream().map(result -> {
            Long categoryId = findCategoryIdByName(result.getCategory());
            return AiParsePreviewVO.builder()
                    .amount(result.getAmount().abs())
                    .category(result.getCategory())
                    .categoryId(categoryId)
                    .date(result.getDate() != null ? result.getDate().toString() : LocalDate.now().toString())
                    .remark(result.getRemark())
                    .candidates(candidates)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public AiParsePreviewVO aiOcrPreview(AiOcrRequest request, Long userId) {
        BillParseResult result;
        try {
            result = aiService.parseBillImage(request.getImageBase64(), userId);
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCode.AI_PARSE_PREVIEW_FAILED);
        }

        Long categoryId = findCategoryIdByName(result.getCategory());

        List<AiParsePreviewVO.CategoryCandidate> candidates = categoryMapper.selectList(null).stream()
                .map(c -> AiParsePreviewVO.CategoryCandidate.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .type(c.getType())
                        .build())
                .collect(Collectors.toList());

        return AiParsePreviewVO.builder()
                .amount(result.getAmount().abs())
                .category(result.getCategory())
                .categoryId(categoryId)
                .date(result.getDate() != null ? result.getDate().toString() : LocalDate.now().toString())
                .remark(result.getRemark())
                .candidates(candidates)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BillVO aiConfirmCreate(AiConfirmRequest request, Long userId) {
        Bill bill = new Bill();
        bill.setUserId(userId);
        bill.setAmount(request.getAmount());
        bill.setType(request.getType());
        bill.setCategoryId(request.getCategoryId());
        bill.setBillDate(LocalDate.parse(request.getDate()));
        bill.setRemark(request.getRemark());
        bill.setInputType(1);
        billMapper.insert(bill);

        checkBudget(userId, bill);

        BillVO vo = convertToVO(bill);
        Map<Long, String> categoryMap = getCategoryMap();
        vo.setCategoryName(categoryMap.getOrDefault(bill.getCategoryId(), "未分类"));
        vo.setTypeName(bill.getType() == 1 ? "收入" : "支出");
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<BillVO> aiBatchConfirmCreate(List<AiConfirmRequest> requests, Long userId) {
        return requests.stream()
                .map(req -> aiConfirmCreate(req, userId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BillVO createBill(BillRequest request, Long userId) {
        validateBillRequest(request);
        Long ledgerId = ledgerService.getCurrentLedgerId(userId);

        Bill bill = new Bill();
        bill.setUserId(userId);
        bill.setLedgerId(ledgerId);
        bill.setAmount(request.getAmount());
        bill.setType(request.getType());
        bill.setCategoryId(request.getCategoryId());
        bill.setBillDate(request.getBillDate() != null ? request.getBillDate() : LocalDate.now());
        bill.setRemark(request.getRemark());
        bill.setInputType(2);
        billMapper.insert(bill);

        checkBudget(userId, bill);

        return convertToVO(bill);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BillVO updateBill(Long id, BillRequest request, Long userId) {
        Bill bill = billMapper.selectById(id);
        if (bill == null) {
            throw new BusinessException(ErrorCode.BILL_NOT_FOUND);
        }
        if (!bill.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.BILL_NO_PERMISSION);
        }

        if (request.getAmount() != null) bill.setAmount(request.getAmount());
        if (request.getType() != null) bill.setType(request.getType());
        if (request.getCategoryId() != null) bill.setCategoryId(request.getCategoryId());
        if (request.getBillDate() != null) bill.setBillDate(request.getBillDate());
        if (request.getRemark() != null) bill.setRemark(request.getRemark());
        billMapper.updateById(bill);

        checkBudget(userId, bill);

        return convertToVO(bill);
    }

    private void checkBudget(Long userId, Bill bill) {
        if (bill.getType() != 2) return; // 仅检查支出

        String month = bill.getBillDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        
        // 1. 检查全月总预算
        checkCategoryBudget(userId, null, month);
        
        // 2. 检查分类预算
        if (bill.getCategoryId() != null) {
            checkCategoryBudget(userId, bill.getCategoryId(), month);
        }
    }

    private void checkCategoryBudget(Long userId, Long categoryId, String month) {
        LambdaQueryWrapper<Budget> query = new LambdaQueryWrapper<Budget>()
                .eq(Budget::getUserId, userId)
                .eq(Budget::getMonth, month);
        if (categoryId != null) {
            query.eq(Budget::getCategoryId, categoryId);
        } else {
            query.isNull(Budget::getCategoryId);
        }
        
        Budget budget = budgetMapper.selectOne(query);
        if (budget == null || budget.getAmount().compareTo(BigDecimal.ZERO) <= 0) return;

        // 计算已用金额
        LocalDate start = LocalDate.parse(month + "-01");
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        
        LambdaQueryWrapper<Bill> billQuery = new LambdaQueryWrapper<Bill>()
                .eq(Bill::getUserId, userId)
                .eq(Bill::getType, 2)
                .ge(Bill::getBillDate, start)
                .le(Bill::getBillDate, end);
        if (categoryId != null) billQuery.eq(Bill::getCategoryId, categoryId);
        
        BigDecimal used = billMapper.selectList(billQuery).stream()
                .map(Bill::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double ratio = used.divide(budget.getAmount(), 4, BigDecimal.ROUND_HALF_UP).doubleValue();
        String catName = categoryId == null ? "总支出" : categoryMapper.selectById(categoryId).getName();

        if (ratio >= 1.0) {
            notificationService.createNotification(userId, "🔴 预算超支提醒", 
                String.format("您在 %s 的 [%s] 预算已超支！预算: ¥%s, 已用: ¥%s", month, catName, budget.getAmount(), used), "BUDGET_WARNING");
        } else if (ratio >= 0.8) {
            notificationService.createNotification(userId, "🟡 预算预警", 
                String.format("您在 %s 的 [%s] 预算消耗已达 %.0f%%。请合理安排后续开支。", month, catName, ratio * 100), "BUDGET_WARNING");
        }
    }

    @Override
    public void deleteBill(Long id, Long userId) {
        Bill bill = billMapper.selectById(id);
        if (bill == null) {
            throw new BusinessException(ErrorCode.BILL_NOT_FOUND);
        }
        if (!bill.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.BILL_NO_PERMISSION);
        }
        billMapper.deleteById(id);
    }

    @Override
    public void batchDeleteBills(List<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            Bill bill = billMapper.selectById(id);
            if (bill != null && bill.getUserId().equals(userId)) {
                billMapper.deleteById(id);
            }
        }
    }

    @Override
    public Page<BillVO> listBills(Long userId, Integer type, Long categoryId,
                                   String startDate, String endDate,
                                   String searchText, BigDecimal minAmount, BigDecimal maxAmount,
                                   int pageNum, int pageSize) {
        Long ledgerId = ledgerService.getCurrentLedgerId(userId);
        Page<Bill> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bill::getUserId, userId)
               .eq(Bill::getLedgerId, ledgerId);
        if (type != null) wrapper.eq(Bill::getType, type);
        if (categoryId != null) wrapper.eq(Bill::getCategoryId, categoryId);
        if (startDate != null) wrapper.ge(Bill::getBillDate, LocalDate.parse(startDate));
        if (endDate != null) wrapper.le(Bill::getBillDate, LocalDate.parse(endDate));
        if (searchText != null && !searchText.trim().isEmpty()) {
            wrapper.like(Bill::getRemark, searchText);
        }
        if (minAmount != null) wrapper.ge(Bill::getAmount, minAmount);
        if (maxAmount != null) wrapper.le(Bill::getAmount, maxAmount);
        wrapper.orderByDesc(Bill::getBillDate, Bill::getCreatedAt);

        Page<Bill> billPage = billMapper.selectPage(page, wrapper);
        Map<Long, String> categoryMap = getCategoryMap();

        Page<BillVO> voPage = new Page<>(billPage.getCurrent(), billPage.getSize(), billPage.getTotal());
        List<BillVO> voList = billPage.getRecords().stream()
                .map(bill -> {
                    BillVO vo = convertToVO(bill);
                    vo.setCategoryName(categoryMap.getOrDefault(bill.getCategoryId(), "未分类"));
                    vo.setTypeName(bill.getType() == 1 ? "收入" : "支出");
                    return vo;
                })
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public BillVO getBill(Long id, Long userId) {
        Long ledgerId = ledgerService.getCurrentLedgerId(userId);
        Bill bill = billMapper.selectById(id);
        if (bill == null) {
            throw new BusinessException(ErrorCode.BILL_NOT_FOUND);
        }
        if (!bill.getUserId().equals(userId) || !bill.getLedgerId().equals(ledgerId)) {
            throw new BusinessException(ErrorCode.BILL_NO_PERMISSION);
        }
        BillVO vo = convertToVO(bill);
        Map<Long, String> categoryMap = getCategoryMap();
        vo.setCategoryName(categoryMap.getOrDefault(bill.getCategoryId(), "未分类"));
        vo.setTypeName(bill.getType() == 1 ? "收入" : "支出");
        return vo;
    }

    private void validateBillRequest(BillRequest request) {
        if (request.getAmount() == null || request.getAmount().doubleValue() <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "金额必须大于0");
        }
        if (request.getType() != null && request.getType() != 1 && request.getType() != 2) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "类型必须为1(收入)或2(支出)");
        }
    }

    private Long findCategoryIdByName(String categoryName) {
        if (categoryName == null) return null;
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getName, categoryName).isNull(Category::getUserId);
        Category category = categoryMapper.selectOne(wrapper);
        return category != null ? category.getId() : null;
    }

    private Map<Long, String> getCategoryMap() {
        return categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }

    private BillVO convertToVO(Bill bill) {
        return BillVO.builder()
                .id(bill.getId())
                .amount(bill.getAmount())
                .type(bill.getType())
                .categoryId(bill.getCategoryId())
                .billDate(bill.getBillDate())
                .remark(bill.getRemark())
                .inputType(bill.getInputType())
                .createdAt(bill.getCreatedAt())
                .build();
    }

}
