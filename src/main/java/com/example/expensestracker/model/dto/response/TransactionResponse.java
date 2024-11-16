package com.example.expensestracker.model.dto.response;

import com.example.expensestracker.model.entity.CategoryEntity;
import com.example.expensestracker.model.entity.TransactionEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionResponse {
    @JsonProperty("transaction_id")
    private Long transactionId;
    private String categoryName;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String note;

    public static TransactionResponse fromEntity(TransactionEntity transactionEntity) {
        TransactionResponse transactionResponse = TransactionResponse.builder()
                .transactionId(transactionEntity.getTransactionId())
                .categoryName(transactionEntity.getCategory().getCategoryName())
                .amount(transactionEntity.getAmount())
                .transactionDate(transactionEntity.getTransactionDate())
                .note(transactionEntity.getNote())
                .build();
        return transactionResponse;
    }
}
