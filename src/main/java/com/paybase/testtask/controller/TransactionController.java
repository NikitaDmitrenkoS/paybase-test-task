package com.paybase.testtask.controller;

import com.paybase.testtask.dto.TransactionDetailsResponse;
import com.paybase.testtask.dto.TransactionRequest;
import com.paybase.testtask.dto.TransactionResponse;
import com.paybase.testtask.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Transaction processing for accounts.")
public class TransactionController {

    private final TransactionService service;

    @PostMapping
    @Operation(summary = "Create transaction", description = "Creates a deposit, withdrawal, transfer, fee, or refund.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "409", description = "Insufficient funds")
    })
    public TransactionResponse create(
            @Valid @RequestBody TransactionRequest r) {

        var tx = service.create(r);
        return TransactionResponse.from(tx);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction", description = "Returns details for a single transaction.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction retrieved"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    public TransactionDetailsResponse get(@PathVariable Long id) {
        return TransactionDetailsResponse.from(service.get(id));
    }
}
