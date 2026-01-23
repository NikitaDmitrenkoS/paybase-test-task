package com.paybase.testtask.service;

import com.paybase.testtask.domain.AccountEntity;
import com.paybase.testtask.dto.TransactionRequest;
import com.paybase.testtask.domain.TransactionEntity;
import com.paybase.testtask.repository.AccountRepository;
import com.paybase.testtask.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accountRepo;
    private final TransactionRepository txRepo;

    @Transactional
    public TransactionEntity create(TransactionRequest r) {

        return txRepo.findByIdempotencyKey(r.idempotencyKey())
                .orElseGet(() -> execute(r));
    }

    private TransactionEntity execute(TransactionRequest r) {

        return switch (r.type()) {
            case DEPOSIT, REFUND -> deposit(r);
            case WITHDRAWAL, FEE -> withdraw(r);
            case TRANSFER -> transfer(r);
        };
    }

    private TransactionEntity deposit(TransactionRequest r) {

        var acc = accountRepo.lockById(r.toAccountId()).orElseThrow();

        var before = acc.getBalance();
        acc.credit(r.amount());

        return saveTx(r, null, acc, null, null, before, acc.getBalance());
    }

    private TransactionEntity withdraw(TransactionRequest r) {

        var acc = accountRepo.lockById(r.fromAccountId()).orElseThrow();

        var before = acc.getBalance();
        acc.debit(r.amount());

        return saveTx(r, acc, null, before, acc.getBalance());
    }

    private TransactionEntity transfer(TransactionRequest r) {

        var from = accountRepo.lockById(r.fromAccountId()).orElseThrow();
        var to = accountRepo.lockById(r.toAccountId()).orElseThrow();

        var fromBefore = from.getBalance();
        var toBefore = to.getBalance();

        from.debit(r.amount());
        to.credit(r.amount());

        return saveTx(r, from, to,
                fromBefore, from.getBalance(),
                toBefore, to.getBalance());
    }

    private TransactionEntity saveTx(
            TransactionRequest r,
            AccountEntity from,
            AccountEntity to,
            BigDecimal fromBefore,
            BigDecimal fromAfter
    ) {
        return saveTx(r, from, to, fromBefore, fromAfter, null, null);
    }

    private TransactionEntity saveTx(
            TransactionRequest r,
            AccountEntity from,
            AccountEntity to,
            BigDecimal fromBefore,
            BigDecimal fromAfter,
            BigDecimal toBefore,
            BigDecimal toAfter
    ) {
        TransactionEntity tx = new TransactionEntity();

        tx.setIdempotencyKey(r.idempotencyKey());
        tx.setType(r.type());
        tx.setFromAccountId(from != null ? from.getId() : null);
        tx.setToAccountId(to != null ? to.getId() : null);
        tx.setAmount(r.amount());
        tx.setCurrency(r.currency());
        tx.setStatus("COMPLETED");

        tx.setFromBalanceBefore(fromBefore);
        tx.setFromBalanceAfter(fromAfter);

        tx.setToBalanceBefore(toBefore);
        tx.setToBalanceAfter(toAfter);

        tx.setReference(r.reference());
        tx.setCreatedAt(Instant.now());

        return txRepo.save(tx);
    }

}
