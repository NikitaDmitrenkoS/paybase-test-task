package com.paybase.testtask.dto;

import com.paybase.testtask.domain.TransactionEntity;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long transactionId,
        String status,
        BigDecimal fromBalance,
        BigDecimal toBalance,
        Instant createdAt
) {

    public static TransactionResponse from(TransactionEntity tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getStatus(),
                tx.getFromBalanceAfter(),
                tx.getToBalanceAfter(),
                tx.getCreatedAt()
        );
    }
}
