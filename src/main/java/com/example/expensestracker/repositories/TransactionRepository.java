package com.example.expensestracker.repositories;

import com.example.expensestracker.model.entity.CategoryEntity;
import com.example.expensestracker.model.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long>, JpaSpecificationExecutor<TransactionEntity> {
    @Query("SELECT t FROM TransactionEntity t WHERE t.transactionId = ?1 AND t.user.userId = ?2")
    Optional<TransactionEntity> findByTransactionIdAndUserId(Long transactionId, Long userId);

    @Query("SELECT t FROM TransactionEntity t WHERE t.user.userId = ?1 AND t.transactionDate BETWEEN  ?2 AND  ?3 ORDER BY t.transactionDate ASC")
    List<TransactionEntity> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT t.transactionDate, " +
            "SUM(CASE WHEN t.category.type = 'income' THEN t.amount ELSE 0 END) AS totalIncome, " +
            "SUM(CASE WHEN t.category.type = 'expense' THEN t.amount ELSE 0 END) AS totalExpense " +
            "FROM TransactionEntity t " +
            "WHERE t.user.userId = :userId AND FUNCTION('MONTH', t.transactionDate) = :month AND FUNCTION('YEAR', t.transactionDate) = :year " +
            "GROUP BY t.transactionDate" +
            " ORDER BY t.transactionDate ASC ")
    List<Object[]> findDailyIncomeAndExpenseByType(@Param("userId") Long userId,
                                                           @Param("month") int month,
                                                           @Param("year") int year);
    @Query("SELECT COUNT(t) > 0 FROM TransactionEntity t WHERE t.user.userId = ?1 AND t.transactionDate = ?2 AND t.category.categoryId = ?3")
    boolean existsByUserIdAndTransactionDateAndCategoryId(Long userId, LocalDate transactionDate, Long categoryId);
    @Modifying
    @Query("DELETE FROM TransactionEntity t WHERE t.user.userId = :userId AND t.fixedTransaction.fixedTransactionId = :fixedTransactionId AND t.transactionDate BETWEEN :startDate AND :endDate")
    void deleteByUserIdAndFixedTransactionIdAndTransactionDateBetween(Long userId, Long fixedTransactionId, LocalDate startDate, LocalDate endDate);
    @Query("SELECT SUM(t.amount) FROM TransactionEntity t WHERE t.user.userId = ?1 AND t.category.categoryId = ?2 AND MONTH(t.transactionDate) = ?3 AND YEAR(t.transactionDate) = ?4")
    BigDecimal sumSpentByCategoryAndUser(Long userId, Long categoryId, int month, int year);
    @Query("SELECT SUM(t.amount) FROM TransactionEntity t WHERE t.user.userId = ?1 AND MONTH(t.transactionDate) = ?2 AND YEAR(t.transactionDate) = ?3 AND t.category.type = 'income'")
    BigDecimal sumSpentByIncomeAndUser(Long userId, int month, int year);
}
