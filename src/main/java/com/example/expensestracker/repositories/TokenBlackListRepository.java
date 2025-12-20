package com.example.expensestracker.repositories;

import com.example.expensestracker.model.entity.TokenBlackList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenBlackListRepository extends JpaRepository<TokenBlackList, Long> {
    boolean existsByToken(String token);
}
