package com.example.expensestracker.service;

import com.example.expensestracker.model.dto.response.CategoryReport;
import com.example.expensestracker.model.dto.response.MonthlyReportResponse;
import com.example.expensestracker.model.entity.CategoryLimitEntity;
import com.example.expensestracker.repositories.CategoryLimitRepository;
import com.example.expensestracker.repositories.CategoryRepository;
import com.example.expensestracker.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService implements IReportService {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryLimitRepository categoryLimitRepository;
    @Override
    public MonthlyReportResponse getMonthlyReport(Long userId, int month, int year) {
        // Tổng thu nhập
        BigDecimal totalIncome = transactionRepository.sumIncomeByUserAndMonthAndYear(userId, month, year);
        if (totalIncome == null) totalIncome = BigDecimal.ZERO;

        // Tổng chi tiêu
        BigDecimal totalExpense = transactionRepository.sumExpenseByUserAndMonthAndYear(userId, month, year);
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;

        // Tổng thu - chi
        BigDecimal netAmount = totalIncome.subtract(totalExpense);

        // Lấy danh sách giới hạn chi tiêu theo danh mục
        List<CategoryLimitEntity> categoryLimits = categoryLimitRepository.findByUserIdAndMonthAndYear(userId, month, year);

        // Tính toán danh sách chi tiêu theo danh mục
        BigDecimal finalTotalIncome = totalIncome;
        BigDecimal finalTotalExpense = totalExpense;
        List<CategoryReport> categoryReports = categoryLimits.stream().map(limit -> {
            BigDecimal spentAmount = transactionRepository.sumSpentByCategoryAndUser(userId, limit.getCategory().getCategoryId(), month, year);
            if (spentAmount == null) spentAmount = BigDecimal.ZERO;

            //BigDecimal percentSpent = spentAmount.multiply(BigDecimal.valueOf(100)).divide(limit.getPercentLimit(), 2, RoundingMode.HALF_UP);
            BigDecimal percentLimit = limit.getPercentLimit();
            BigDecimal percentSpent;
            if (finalTotalIncome.compareTo(BigDecimal.ZERO) == 0 || percentLimit.compareTo(BigDecimal.ZERO) == 0) {
                percentSpent = BigDecimal.ZERO;
            } else if ("tiết kiệm".equalsIgnoreCase(limit.getCategory().getCategoryName())) {
                // Nếu là danh mục "tiết kiệm"
                BigDecimal savings = finalTotalIncome.subtract(finalTotalExpense);
                if (savings.compareTo(BigDecimal.ZERO) < 0) {
                    percentSpent = BigDecimal.ZERO; // Nếu tiết kiệm là số âm
                } else {
                    percentSpent = savings
                            .multiply(BigDecimal.valueOf(10000))
                            .divide(finalTotalIncome.multiply(percentLimit), 2, RoundingMode.HALF_UP);
                }
            } else {
                // Các danh mục khác
                percentSpent = spentAmount
                        .multiply(BigDecimal.valueOf(10000))
                        .divide(finalTotalIncome.multiply(percentLimit), 2, RoundingMode.HALF_UP);
            }

            return new CategoryReport(
                    limit.getCategory().getCategoryId(),
                    limit.getCategory().getCategoryName(),
                    spentAmount,
                    percentSpent,
                    percentLimit
            );
        }).collect(Collectors.toList());

        return new MonthlyReportResponse(totalIncome, totalExpense, netAmount, categoryReports);
    }
}
