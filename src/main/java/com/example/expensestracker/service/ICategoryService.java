package com.example.expensestracker.service;

import com.example.expensestracker.model.dto.request.CategoryDTO;
import com.example.expensestracker.model.dto.response.CategoryResponse;
import com.example.expensestracker.model.entity.CategoryEntity;


import java.util.List;

public interface ICategoryService {
    CategoryEntity createCategory(CategoryDTO category,Long userId);
    CategoryEntity updateCategory(Long categoryId,Long userId, CategoryDTO categoryDTO);
    void deleteCategory(Long categoryId,Long userId);

    CategoryEntity getCategoryById(Long id);
    List<CategoryEntity> getAllCategories();

    List<CategoryResponse> getExpenseCategories(Long userId);
    List<CategoryResponse> getIncomeCategories(Long userId);
}
