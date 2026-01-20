package com.paybase.testtask.service;

import com.paybase.testtask.domain.AccountEntity;
import com.paybase.testtask.domain.AccountStatus;
import com.paybase.testtask.domain.TransactionEntity;
import com.paybase.testtask.dto.BalanceResponse;
import com.paybase.testtask.dto.CreateAccountRequest;
import com.paybase.testtask.exceptions.NotFoundException;
import com.paybase.testtask.repository.AccountRepository;
import com.paybase.testtask.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public List<TransactionEntity> statement(Long accountId) {

        accountRepository
                .findByIdForRead(accountId)
                .orElseThrow(NotFoundException::new);

        return transactionRepository
                .findAllByFromAccountIdOrToAccountIdOrderByCreatedAt(
                        accountId, accountId
                );
    }
}
