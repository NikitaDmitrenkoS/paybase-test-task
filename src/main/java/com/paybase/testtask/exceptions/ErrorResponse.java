package com.paybase.testtask.exceptions;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String message,
        Instant timestamp,
        Map<String, String> errors
) {
}
