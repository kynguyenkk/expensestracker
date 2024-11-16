package com.example.expensestracker.service;

import com.example.expensestracker.model.dto.response.CategoryResponse;
import com.example.expensestracker.model.dto.response.DailyTransactionResponse;
import com.example.expensestracker.model.dto.response.MonthlyTransactionResponse;
import com.example.expensestracker.model.dto.response.TransactionResponse;
import com.example.expensestracker.model.entity.TransactionEntity;
import com.example.expensestracker.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FinanceService implements IFinanceService {
    @Autowired
    private TransactionRepository transactionRepository;

    public MonthlyTransactionResponse getMonthlyData(Long userId, int month, int year) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<TransactionEntity> transactions = transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate);

        BigDecimal totalIncome = BigDecimal.valueOf(0);
        BigDecimal totalExpense = BigDecimal.valueOf(0);

        for (TransactionEntity transaction : transactions) {
            String categoryType = transaction.getCategory().getCategoryType().name(); // Giả sử categoryType là ENUM với các giá trị "INCOME" và "EXPENSE"

            if ("income".equals(categoryType)) {
                totalIncome = totalIncome.add(transaction.getAmount());
            } else if ("expense".equals(categoryType)) {
                totalExpense = totalExpense.add(transaction.getAmount().abs());
            }
        }

        List<Object[]> results = transactionRepository.findDailyIncomeAndExpenseByCategoryType(userId, month, year);

        Map<LocalDate, DailyTransactionResponse> dailyTransactions = new HashMap<>();

        for (Object[] result : results) {
            LocalDate date = (LocalDate) result[0];
            BigDecimal totalDailyIncome = (BigDecimal) result[1];
            BigDecimal totalDailyExpense = (BigDecimal) result[2];

            dailyTransactions.put(date, new DailyTransactionResponse(totalDailyIncome, totalDailyExpense));
        }

        BigDecimal balance = totalIncome.subtract(totalExpense);
        List<TransactionResponse> transactionResponses = transactions.stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
        return new MonthlyTransactionResponse(dailyTransactions,totalIncome, totalExpense, balance, transactionResponses);
    }

}
