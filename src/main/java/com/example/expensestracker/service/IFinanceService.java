package com.example.expensestracker.service;

import com.example.expensestracker.model.dto.response.MonthlyTransactionResponse;

public interface IFinanceService {
    public MonthlyTransactionResponse getMonthlyData(Long userId, int month, int year);
}
