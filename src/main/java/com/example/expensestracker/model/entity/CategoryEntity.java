package com.example.expensestracker.model.entity;

import com.example.expensestracker.model.enums.CategoryType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="categories")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;
    @Column(name="category_name",length = 50,nullable = false)
    private String categoryName;
    @Column(name="category_type",nullable = false)
    @Enumerated(EnumType.STRING)
    private CategoryType categoryType;
    @Column(name="is_default")
    private boolean isDefault;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id")
    private UserEntity user;
}
