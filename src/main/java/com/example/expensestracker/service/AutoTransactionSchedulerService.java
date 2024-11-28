package com.example.expensestracker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

public class AutoTransactionSchedulerService {
    @Autowired
    private FixedTransactionService fixedTransactionService;

    /**
     * Scheduler chạy mỗi ngày lúc 00:00 để tạo giao dịch tự động.
     */
    @Scheduled(cron = "0 0 0 * * ?") // Mỗi ngày vào lúc 00:00
    public void generateDailyTransactions() {
        try {
            fixedTransactionService.generateTransactionsForToday();
            System.out.println("Giao dịch tự động đã được tạo thành công!");
        } catch (Exception e) {
            System.err.println("Lỗi khi tạo giao dịch tự động: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
