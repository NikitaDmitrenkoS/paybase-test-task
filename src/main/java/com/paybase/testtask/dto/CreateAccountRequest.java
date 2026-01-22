package com.paybase.testtask.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank String merchantId,
        @NotBlank String currency,
        @NotNull @DecimalMin("0.0000") BigDecimal initialBalance
) {
}
