package com.paybase.testtask.dto;

import com.paybase.testtask.domain.AccountEntity;
import com.paybase.testtask.domain.AccountStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        Long accountId,
        String merchantId,
        BigDecimal balance,
        String currency,
        AccountStatus status,
        Instant createdAt
) {
    public static AccountResponse from(AccountEntity account) {
        return new AccountResponse(
                account.getId(),
                account.getMerchantId(),
                account.getBalance(),
                account.getCurrency(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}
