package com.example.expensestracker.service;

import com.example.expensestracker.model.dto.request.FixedTransactionDTO;
import com.example.expensestracker.model.dto.request.TransactionDTO;
import com.example.expensestracker.model.dto.response.FixedTransactionResponse;
import com.example.expensestracker.model.entity.FixedTransactionEntity;
import com.example.expensestracker.model.entity.TransactionEntity;
import jakarta.validation.Valid;

import java.util.List;

public interface IFixedTransactionService {
    FixedTransactionEntity createFixedTransaction(FixedTransactionDTO fixedTransactionDTO, Long userId) throws Exception;
    FixedTransactionEntity updateFixedTransaction(Long fixedTransactionId, Long userId, FixedTransactionDTO fixedTransactionDTO) throws Exception;
    void deleteFixedTransaction(Long fixedTransactionId, Long userId) throws Exception;
    List<FixedTransactionResponse> getFixedTransaction(Long userId);
}
