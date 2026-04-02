package com.aibookkeeping.service.bill;

import com.aibookkeeping.dto.AiParseRequest;
import com.aibookkeeping.dto.BillRequest;
import com.aibookkeeping.vo.AiParseVO;
import com.aibookkeeping.vo.BillVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface BillService {

    /**
     * AI 解析自然语言并记账
     */
    AiParseVO aiParseAndCreate(AiParseRequest request, Long userId);

    /**
     * 手动创建账单
     */
    BillVO createBill(BillRequest request, Long userId);

    /**
     * 更新账单
     */
    BillVO updateBill(Long id, BillRequest request, Long userId);

    /**
     * 删除账单（软删除）
     */
    void deleteBill(Long id, Long userId);

    /**
     * 获取账单列表（分页+筛选）
     */
    Page<BillVO> listBills(Long userId, Integer type, Long categoryId,
                           String startDate, String endDate, int pageNum, int pageSize);

    /**
     * 获取账单详情
     */
    BillVO getBill(Long id, Long userId);
}
