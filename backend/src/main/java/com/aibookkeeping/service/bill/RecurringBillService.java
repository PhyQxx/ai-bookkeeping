package com.aibookkeeping.service.bill;

import com.aibookkeeping.dto.RecurringBillRequest;
import com.aibookkeeping.vo.RecurringBillVO;

import java.util.List;

public interface RecurringBillService {

    List<RecurringBillVO> listRecurringBills(Long userId);

    RecurringBillVO createRecurringBill(RecurringBillRequest request, Long userId);

    RecurringBillVO updateRecurringBill(Long id, RecurringBillRequest request, Long userId);

    void deleteRecurringBill(Long id, Long userId);

    void generateBills();
}
