package com.paybase.testtask.domain;


import com.paybase.testtask.exceptions.InsufficientFundsException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "accounts",
        indexes = {
                @Index(name = "idx_account_merchant", columnList = "merchant_id")
        }
)
@Getter
@Setter
@NoArgsConstructor

public class AccountEntity {
    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }
        balance = balance.subtract(amount);
    }

}
