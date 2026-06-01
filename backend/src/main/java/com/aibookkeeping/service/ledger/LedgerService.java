package com.aibookkeeping.service.ledger;

import com.aibookkeeping.dto.LedgerRequest;
import com.aibookkeeping.vo.LedgerVO;

import java.util.List;

public interface LedgerService {
    List<LedgerVO> listLedgers(Long userId);
    LedgerVO createLedger(LedgerRequest request, Long userId);
    LedgerVO updateLedger(Long id, LedgerRequest request, Long userId);
    void deleteLedger(Long id, Long userId);
    void switchLedger(Long id, Long userId);
    Long getCurrentLedgerId(Long userId);
}
