package com.paybase.testtask.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record BalanceResponse(
        Long accountId,
        BigDecimal balance,
        String currency,
        Instant lastUpdated
) {
}
