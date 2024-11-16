package com.example.expensestracker.model.dto.response;


import com.example.expensestracker.model.entity.CategoryEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategoryResponse {
    @JsonProperty("category_id")
    private Long categoryId;
    private String categoryName;
    private String categoryType;
    private Boolean isDefault;

    public static CategoryResponse fromCategory(CategoryEntity categories) {
        CategoryResponse categoryResponse = CategoryResponse.builder()
                .categoryId(categories.getCategoryId())
                .categoryName(categories.getCategoryName())
                .categoryType(String.valueOf(categories.getCategoryType()))
                .isDefault(categories.isDefault())
                .build();
        return categoryResponse;
    }
}
