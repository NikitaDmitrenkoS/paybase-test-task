package com.paybase.testtask.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Paybase Internal Balances API",
                description = "REST API for managing merchant accounts and transactions.",
                version = "v1"
        )
)
public class OpenApiConfig {
}
