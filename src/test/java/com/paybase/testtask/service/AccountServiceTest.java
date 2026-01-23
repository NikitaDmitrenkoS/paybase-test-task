package com.paybase.testtask.service;

import com.paybase.testtask.domain.AccountEntity;
import com.paybase.testtask.domain.AccountStatus;
import com.paybase.testtask.domain.TransactionEntity;
import com.paybase.testtask.dto.CreateAccountRequest;
import com.paybase.testtask.exceptions.NotFoundException;
import com.paybase.testtask.repository.AccountRepository;
import com.paybase.testtask.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createPersistsAccountWithDefaults() {
        CreateAccountRequest request = new CreateAccountRequest(
                "merchant-1",
                "USD",
                new BigDecimal("25.00")
        );

        when(accountRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AccountEntity result = accountService.create(request);

        ArgumentCaptor<AccountEntity> captor = ArgumentCaptor.forClass(AccountEntity.class);
        verify(accountRepository).save(captor.capture());

        AccountEntity saved = captor.getValue();
        assertThat(saved.getMerchantId()).isEqualTo("merchant-1");
        assertThat(saved.getCurrency()).isEqualTo("USD");
        assertThat(saved.getBalance()).isEqualByComparingTo("25.00");
        assertThat(saved.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(result).isSameAs(saved);
    }

    @Test
    void balanceThrowsNotFoundWhenAccountMissing() {
        when(accountRepository.findByIdForRead(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.balance(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void statementReturnsTransactionsForAccount() {
        AccountEntity account = new AccountEntity();
        account.setId(10L);
        account.setCreatedAt(Instant.now());

        TransactionEntity transaction = new TransactionEntity();
        transaction.setId(5L);

        when(accountRepository.findByIdForRead(10L)).thenReturn(Optional.of(account));
        when(transactionRepository.findAllByFromAccountIdOrToAccountIdOrderByCreatedAt(10L, 10L))
                .thenReturn(List.of(transaction));

        List<TransactionEntity> result = accountService.statement(10L);

        assertThat(result).containsExactly(transaction);
    }
}
