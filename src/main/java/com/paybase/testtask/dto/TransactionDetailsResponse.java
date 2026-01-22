package com.paybase.testtask.dto;

import com.paybase.testtask.domain.TransactionEntity;
import com.paybase.testtask.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionDetailsResponse(
        Long transactionId,
        TransactionType type,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        String currency,
        String status,
        BigDecimal fromBalanceBefore,
        BigDecimal fromBalanceAfter,
        BigDecimal toBalanceBefore,
        BigDecimal toBalanceAfter,
        String reference,
        Instant createdAt
) {
    public static TransactionDetailsResponse from(TransactionEntity tx) {
        return new TransactionDetailsResponse(
                tx.getId(),
                tx.getType(),
                tx.getFromAccountId(),
                tx.getToAccountId(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getStatus(),
                tx.getFromBalanceBefore(),
                tx.getFromBalanceAfter(),
                tx.getToBalanceBefore(),
                tx.getToBalanceAfter(),
                tx.getReference(),
                tx.getCreatedAt()
        );
    }
}
