package com.aibookkeeping.service.bill;

import com.aibookkeeping.dto.AiConfirmRequest;
import com.aibookkeeping.dto.AiOcrRequest;
import com.aibookkeeping.dto.AiParseRequest;
import com.aibookkeeping.dto.BillRequest;
import com.aibookkeeping.vo.AiParsePreviewVO;
import com.aibookkeeping.vo.AiParseVO;
import com.aibookkeeping.vo.BillVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface BillService {

    AiParseVO aiParseAndCreate(AiParseRequest request, Long userId);

    AiParsePreviewVO aiParsePreview(AiParseRequest request, Long userId);

    List<AiParsePreviewVO> aiBatchParsePreview(AiParseRequest request, Long userId);

    AiParsePreviewVO aiOcrPreview(AiOcrRequest request, Long userId);

    BillVO aiConfirmCreate(AiConfirmRequest request, Long userId);

    List<BillVO> aiBatchConfirmCreate(List<AiConfirmRequest> requests, Long userId);

    BillVO createBill(BillRequest request, Long userId);

    BillVO updateBill(Long id, BillRequest request, Long userId);

    void deleteBill(Long id, Long userId);

    void batchDeleteBills(List<Long> ids, Long userId);

    Page<BillVO> listBills(Long userId, Integer type, Long categoryId,
                           String startDate, String endDate,
                           String searchText, BigDecimal minAmount, BigDecimal maxAmount,
                           int pageNum, int pageSize);

    BillVO getBill(Long id, Long userId);
}
