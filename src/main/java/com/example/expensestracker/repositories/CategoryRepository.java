package com.example.expensestracker.repositories;

import com.example.expensestracker.model.dto.response.CategoryResponse;
import com.example.expensestracker.model.entity.CategoryEntity;
import com.example.expensestracker.model.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
    @Query(value = "SELECT COUNT(c) > 0 FROM CategoryEntity c WHERE c.categoryName = ?1 AND c.user.userId = ?2")
    boolean existsByCategoryNameAndUserId(String categoryName, Long userId);

    // Tìm tất cả các category theo categoryName và isDefault = true
    @Query(value = "SELECT COUNT(c) > 0 FROM CategoryEntity c WHERE c.categoryName = ?1 AND c.categoryType=?2 AND c.isDefault = true ")
    boolean existsByCategoryNameAndCategoryTypeAndIsDefault(String categoryName, CategoryType categoryType, boolean isDefault);

    @Query("SELECT COUNT(c) > 0 FROM CategoryEntity c WHERE c.user.userId = ?1")
    boolean existsByUserId(Long userId);

    @Query("SELECT c FROM CategoryEntity c WHERE c.user.userId = :userId AND c.isDefault = false")
    List<CategoryEntity> findUserCategories(@Param("userId") Long userId);

    @Query("SELECT c FROM CategoryEntity c WHERE c.categoryId = ?1 AND c.user.userId = ?2")
    Optional<CategoryEntity> findByCategoryIdAndUserId(Long categoryId, Long userId);

    @Query("SELECT c FROM CategoryEntity c WHERE c.categoryType = :categoryType AND (c.user.userId = :userId OR c.isDefault = true)")
    List<CategoryEntity> findByCategoryTypeAndUserIdOrIsDefault(@Param("categoryType") CategoryType categoryType, @Param("userId") Long userId);
}
