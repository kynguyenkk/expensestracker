package com.example.expensestracker.model.dto.response;

import com.example.expensestracker.model.entity.FixedTransactionEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FixedTransactionResponse {
    @JsonProperty("fixed_transaction_id")
    private Long fixedTransactionId;
    @JsonProperty("category_id")
    private Long categoryId;
    private String title;
    private String categoryName;
    private BigDecimal amount;
    private LocalDate startDate;
    private LocalDate endDate;
    public static FixedTransactionResponse fromEntity(FixedTransactionEntity fixedTransactionEntity) {
        FixedTransactionResponse fixedTransactionResponse = FixedTransactionResponse.builder()
                .fixedTransactionId(fixedTransactionEntity.getFixedTransactionId())
                .categoryId(fixedTransactionEntity.getCategory().getCategoryId())
                .categoryName(fixedTransactionEntity.getCategory().getCategoryName())
                .amount(fixedTransactionEntity.getAmount())
                .startDate(fixedTransactionEntity.getStartDate())
                .endDate(fixedTransactionEntity.getEndDate())
                .build();
        return fixedTransactionResponse;
    }
}
