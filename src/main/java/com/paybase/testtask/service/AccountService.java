package com.paybase.testtask.service;

import com.paybase.testtask.domain.AccountEntity;
import com.paybase.testtask.domain.AccountStatus;
import com.paybase.testtask.domain.TransactionEntity;
import com.paybase.testtask.dto.BalanceResponse;
import com.paybase.testtask.dto.CreateAccountRequest;
import com.paybase.testtask.dto.StatementEntry;
import com.paybase.testtask.dto.StatementResponse;
import com.paybase.testtask.exceptions.NotFoundException;
import com.paybase.testtask.repository.AccountRepository;
import com.paybase.testtask.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public AccountEntity create(CreateAccountRequest r) {

        AccountEntity account = new AccountEntity();
        account.setMerchantId(r.merchantId());
        account.setCurrency(r.currency());
        account.setBalance(r.initialBalance());
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(Instant.now());

        return accountRepository.save(account);
    }

    public BalanceResponse balance(Long accountId) {

        AccountEntity acc = accountRepository
                .findByIdForRead(accountId)
                .orElseThrow(NotFoundException::new);

        return new BalanceResponse(
                acc.getId(),
                acc.getBalance(),
                acc.getCurrency(),
                Instant.now()
        );
    }

    public StatementResponse statement(
            Long accountId,
            LocalDate from,
            LocalDate to
    ) {

        AccountEntity account = accountRepository
                .findByIdForRead(accountId)
                .orElseThrow(NotFoundException::new);

        Instant fromInstant = from != null
                ? from.atStartOfDay().toInstant(ZoneOffset.UTC)
                : null;
        Instant toInstant = to != null
                ? to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
                : null;

        List<TransactionEntity> transactions = transactionRepository
                .findStatement(accountId, fromInstant, toInstant);

        List<StatementEntry> entries = transactions.stream()
                .map(tx -> toEntry(accountId, tx))
                .toList();

        return new StatementResponse(
                accountId,
                account.getBalance(),
                account.getCurrency(),
                entries
        );
    }

    private StatementEntry toEntry(Long accountId, TransactionEntity tx) {
        boolean isFrom = accountId.equals(tx.getFromAccountId());
        boolean isTo = accountId.equals(tx.getToAccountId());

        if (isFrom) {
            return new StatementEntry(
                    tx.getId(),
                    tx.getType(),
                    tx.getAmount().negate(),
                    tx.getFromBalanceBefore(),
                    tx.getFromBalanceAfter(),
                    tx.getReference(),
                    tx.getCreatedAt()
            );
        }

        if (isTo) {
            return new StatementEntry(
                    tx.getId(),
                    tx.getType(),
                    tx.getAmount(),
                    tx.getToBalanceBefore(),
                    tx.getToBalanceAfter(),
                    tx.getReference(),
                    tx.getCreatedAt()
            );
        }

        return new StatementEntry(
                tx.getId(),
                tx.getType(),
                tx.getAmount(),
                null,
                null,
                tx.getReference(),
                tx.getCreatedAt()
        );
    }
}
