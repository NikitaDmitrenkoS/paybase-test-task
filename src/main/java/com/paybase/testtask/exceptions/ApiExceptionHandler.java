package com.paybase.testtask.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InsufficientFundsException.class)
    ResponseEntity<?> insufficientFunds() {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body("Insufficient funds");
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<?> notFound() {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body("Not found");
    }

}
