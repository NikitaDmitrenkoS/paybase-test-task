package com.paybase.testtask.dto;

import com.paybase.testtask.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransactionRequest(
        @NotBlank String idempotencyKey,
        @NotNull TransactionType type,
        Long fromAccountId,
        Long toAccountId,
        @DecimalMin("0.0001") BigDecimal amount,
        @NotBlank String currency,
        String reference
) {}

