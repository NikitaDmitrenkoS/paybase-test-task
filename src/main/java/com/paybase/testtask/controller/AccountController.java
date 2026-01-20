package com.paybase.testtask.controller;

import com.paybase.testtask.domain.AccountEntity;
import com.paybase.testtask.domain.TransactionEntity;
import com.paybase.testtask.dto.BalanceResponse;
import com.paybase.testtask.dto.CreateAccountRequest;
import com.paybase.testtask.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService service;

    @PostMapping
    public AccountEntity create(@RequestBody CreateAccountRequest r) {
        return service.create(r);
    }

    @GetMapping("/{id}/balance")
    public BalanceResponse balance(@PathVariable Long id) {
        return service.balance(id);
    }

    @GetMapping("/{id}/statement")
    public List<TransactionEntity> statement(
            @PathVariable Long id) {
        return service.statement(id);
    }
}
