package com.paybase.testtask.service;

import com.paybase.testtask.domain.AccountEntity;
import com.paybase.testtask.dto.TransactionRequest;
import com.paybase.testtask.domain.TransactionEntity;
import com.paybase.testtask.exceptions.NotFoundException;
import com.paybase.testtask.repository.AccountRepository;
import com.paybase.testtask.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accountRepo;
    private final TransactionRepository txRepo;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public TransactionEntity create(TransactionRequest r) {

        validateRequest(r);

        return txRepo.findByIdempotencyKey(r.idempotencyKey())
                .orElseGet(() -> execute(r));
    }

    @Transactional(readOnly = true)
    public TransactionEntity getById(Long id) {
        return txRepo.findById(id).orElseThrow(NotFoundException::new);
    }

    private TransactionEntity execute(TransactionRequest r) {

        return switch (r.type()) {
            case DEPOSIT -> deposit(r);
            case WITHDRAWAL -> withdraw(r);
            case TRANSFER -> transfer(r);
            case FEE -> withdraw(r);
            case REFUND -> deposit(r);
        };
    }

    private TransactionEntity deposit(TransactionRequest r) {

        var acc = accountRepo.lockById(r.toAccountId())
                .orElseThrow(NotFoundException::new);
        validateCurrency(acc, r.currency());

        var before = acc.getBalance();
        acc.credit(r.amount());

        return saveTx(r, null, acc, before, acc.getBalance());
    }

    private TransactionEntity withdraw(TransactionRequest r) {

        var acc = accountRepo.lockById(r.fromAccountId())
                .orElseThrow(NotFoundException::new);
        validateCurrency(acc, r.currency());

        var before = acc.getBalance();
        acc.debit(r.amount());

        return saveTx(r, acc, null, before, acc.getBalance());
    }

    private TransactionEntity transfer(TransactionRequest r) {

        Long fromId = r.fromAccountId();
        Long toId = r.toAccountId();

        AccountEntity firstLock = fromId < toId
                ? accountRepo.lockById(fromId).orElseThrow(NotFoundException::new)
                : accountRepo.lockById(toId).orElseThrow(NotFoundException::new);
        AccountEntity secondLock = fromId < toId
                ? accountRepo.lockById(toId).orElseThrow(NotFoundException::new)
                : accountRepo.lockById(fromId).orElseThrow(NotFoundException::new);

        AccountEntity from = firstLock.getId().equals(fromId)
                ? firstLock
                : secondLock;
        AccountEntity to = firstLock.getId().equals(toId)
                ? firstLock
                : secondLock;

        validateCurrency(from, r.currency());
        validateCurrency(to, r.currency());

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

    private void validateRequest(TransactionRequest r) {
        switch (r.type()) {
            case DEPOSIT, REFUND -> {
                requireAccount(r.toAccountId(), "toAccountId");
                ensureNull(r.fromAccountId(), "fromAccountId");
            }
            case WITHDRAWAL, FEE -> {
                requireAccount(r.fromAccountId(), "fromAccountId");
                ensureNull(r.toAccountId(), "toAccountId");
            }
            case TRANSFER -> {
                requireAccount(r.fromAccountId(), "fromAccountId");
                requireAccount(r.toAccountId(), "toAccountId");
                if (r.fromAccountId().equals(r.toAccountId())) {
                    throw new IllegalArgumentException(
                            "fromAccountId and toAccountId must differ");
                }
            }
        }
    }

    private void requireAccount(Long accountId, String fieldName) {
        if (accountId == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    private void ensureNull(Long accountId, String fieldName) {
        if (accountId != null) {
            throw new IllegalArgumentException(fieldName + " must be null");
        }
    }

    private void validateCurrency(AccountEntity account, String currency) {
        if (!account.getCurrency().equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("Currency mismatch for account");
        }
    }

}
