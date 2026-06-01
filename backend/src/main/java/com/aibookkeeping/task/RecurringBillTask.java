package com.aibookkeeping.task;

import com.aibookkeeping.service.bill.RecurringBillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringBillTask {

    private final RecurringBillService recurringBillService;

    /**
     * 每天凌晨 00:05 执行一次，生成当日的周期账单
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void generateRecurringBills() {
        try {
            recurringBillService.generateBills();
        } catch (Exception e) {
            log.error("Failed to generate recurring bills", e);
        }
    }
}
