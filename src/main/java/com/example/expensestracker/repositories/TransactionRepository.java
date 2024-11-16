package com.example.expensestracker.repositories;

import com.example.expensestracker.model.entity.CategoryEntity;
import com.example.expensestracker.model.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long>, JpaSpecificationExecutor<TransactionEntity> {
    @Query("SELECT t FROM TransactionEntity t WHERE t.transactionId = ?1 AND t.user.userId = ?2")
    Optional<TransactionEntity> findByTransactionIdAndUserId(Long transactionId, Long userId);

    @Query("SELECT t FROM TransactionEntity t WHERE t.user.userId = ?1 AND t.transactionDate BETWEEN  ?2 AND  ?3")
    List<TransactionEntity> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT t.transactionDate, " +
            "SUM(CASE WHEN t.category.categoryType = 'income' THEN t.amount ELSE 0 END) AS totalIncome, " +
            "SUM(CASE WHEN t.category.categoryType = 'expense' THEN t.amount ELSE 0 END) AS totalExpense " +
            "FROM TransactionEntity t " +
            "WHERE t.user.userId = :userId AND MONTH(t.transactionDate) = :month AND YEAR(t.transactionDate) = :year " +
            "GROUP BY t.transactionDate")
    List<Object[]> findDailyIncomeAndExpenseByCategoryType(@Param("userId") Long userId,
                                                           @Param("month") int month,
                                                           @Param("year") int year);
}
