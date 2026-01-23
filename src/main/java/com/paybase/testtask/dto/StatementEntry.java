package com.paybase.testtask.dto;

import com.paybase.testtask.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record StatementEntry(
        Long transactionId,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String reference,
        Instant createdAt
) {
}
