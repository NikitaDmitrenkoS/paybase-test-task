package com.paybase.testtask.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "transactions",
        uniqueConstraints = @UniqueConstraint(columnNames = "idempotencyKey")
)
@Getter
@Setter
@NoArgsConstructor
public class TransactionEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private Long fromAccountId;
    private Long toAccountId;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    private String currency;
    private String status;

    private BigDecimal fromBalanceBefore;
    private BigDecimal fromBalanceAfter;

    private BigDecimal toBalanceBefore;
    private BigDecimal toBalanceAfter;

    private String reference;
    private Instant createdAt;
}
