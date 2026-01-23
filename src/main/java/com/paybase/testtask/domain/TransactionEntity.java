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
        uniqueConstraints = @UniqueConstraint(columnNames = "idempotency_key"),
        indexes = {
                @Index(
                        name = "idx_transaction_account",
                        columnList = "from_account_id,to_account_id"
                ),
                @Index(name = "idx_transaction_created", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TransactionEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(name = "from_account_id")
    private Long fromAccountId;

    @Column(name = "to_account_id")
    private Long toAccountId;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private String status;

    @Column(name = "from_balance_before", precision = 19, scale = 4)
    private BigDecimal fromBalanceBefore;

    @Column(name = "from_balance_after", precision = 19, scale = 4)
    private BigDecimal fromBalanceAfter;

    @Column(name = "to_balance_before", precision = 19, scale = 4)
    private BigDecimal toBalanceBefore;

    @Column(name = "to_balance_after", precision = 19, scale = 4)
    private BigDecimal toBalanceAfter;

    private String reference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
