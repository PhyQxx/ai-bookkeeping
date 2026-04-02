package com.aibookkeeping.service.bill;

import com.aibookkeeping.ai.model.BillParseResult;
import com.aibookkeeping.ai.service.AiService;
import com.aibookkeeping.dto.AiParseRequest;
import com.aibookkeeping.dto.BillRequest;
import com.aibookkeeping.entity.Bill;
import com.aibookkeeping.entity.Category;
import com.aibookkeeping.exception.BusinessException;
import com.aibookkeeping.exception.ErrorCode;
import com.aibookkeeping.mapper.BillMapper;
import com.aibookkeeping.mapper.CategoryMapper;
import com.aibookkeeping.vo.AiParseVO;
import com.aibookkeeping.vo.BillVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillServiceImpl implements BillService {

    private final BillMapper billMapper;
    private final CategoryMapper categoryMapper;
    private final AiService aiService;

    @Override
    public AiParseVO aiParseAndCreate(AiParseRequest request, Long userId) {
        BillParseResult result = aiService.parseBillInput(request.getInput(), userId);

        Long categoryId = findCategoryIdByName(result.getCategory());

        Bill bill = new Bill();
        bill.setUserId(userId);
        bill.setAmount(result.getAmount().abs());
        bill.setType(result.getAmount().compareTo(BigDecimal.ZERO) >= 0 ? 2 : 1);
        bill.setCategoryId(categoryId);
        bill.setBillDate(result.getDate() != null ? result.getDate() : LocalDate.now());
        bill.setRemark(result.getRemark());
        bill.setInputType(1);
        billMapper.insert(bill);

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
    public BillVO createBill(BillRequest request, Long userId) {
        validateBillRequest(request);

        Bill bill = new Bill();
        bill.setUserId(userId);
        bill.setAmount(request.getAmount());
        bill.setType(request.getType());
        bill.setCategoryId(request.getCategoryId());
        bill.setBillDate(request.getBillDate() != null ? request.getBillDate() : LocalDate.now());
        bill.setRemark(request.getRemark());
        bill.setInputType(2);
        billMapper.insert(bill);
        return convertToVO(bill);
    }

    @Override
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
        return convertToVO(bill);
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
                                   String startDate, String endDate, int pageNum, int pageSize) {
        Page<Bill> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bill::getUserId, userId);
        if (type != null) wrapper.eq(Bill::getType, type);
        if (categoryId != null) wrapper.eq(Bill::getCategoryId, categoryId);
        if (startDate != null) wrapper.ge(Bill::getBillDate, LocalDate.parse(startDate));
        if (endDate != null) wrapper.le(Bill::getBillDate, LocalDate.parse(endDate));
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
        Bill bill = billMapper.selectById(id);
        if (bill == null) {
            throw new BusinessException(ErrorCode.BILL_NOT_FOUND);
        }
        if (!bill.getUserId().equals(userId)) {
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
