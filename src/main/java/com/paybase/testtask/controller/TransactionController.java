package com.paybase.testtask.controller;

import com.paybase.testtask.dto.TransactionRequest;
import com.paybase.testtask.dto.TransactionResponse;
import com.paybase.testtask.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;

    @PostMapping
    public TransactionResponse create(
            @Valid @RequestBody TransactionRequest r) {

        var tx = service.create(r);
        return TransactionResponse.from(tx);
    }
}
