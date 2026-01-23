package com.paybase.testtask.controller;

import com.paybase.testtask.dto.AccountResponse;
import com.paybase.testtask.dto.BalanceResponse;
import com.paybase.testtask.dto.CreateAccountRequest;
import com.paybase.testtask.dto.StatementResponse;
import com.paybase.testtask.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "Account management and statements.")
public class AccountController {

    private final AccountService service;

    @PostMapping
    @Operation(summary = "Create account", description = "Creates a new merchant account with an initial balance.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account created"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest r) {
        return AccountResponse.from(service.create(r));
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Get account balance", description = "Returns the current balance for an account.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balance retrieved"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public BalanceResponse balance(
            @Parameter(description = "Account identifier", example = "123")
            @PathVariable Long id) {
        return service.balance(id);
    }

    @GetMapping("/{id}/statement")
    @Operation(summary = "Get account statement", description = "Returns transactions for an account.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statement retrieved"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public StatementResponse statement(
            @Parameter(description = "Account identifier", example = "123")
            @PathVariable Long id,
            @Parameter(description = "Statement start date (inclusive)", example = "2026-01-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @Parameter(description = "Statement end date (inclusive)", example = "2026-01-31")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to) {
        return service.statement(id, from, to);
    }
}
