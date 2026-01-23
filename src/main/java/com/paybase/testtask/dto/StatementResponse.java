package com.paybase.testtask.dto;

import java.math.BigDecimal;
import java.util.List;

public record StatementResponse(
        Long accountId,
        BigDecimal currentBalance,
        String currency,
        List<StatementEntry> transactions
) {
}
