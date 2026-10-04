package com.fraudtriage.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record TransactionRequest(
    @NotBlank @Size(max=64) String externalRef,
    @NotBlank @Size(max=64) String accountId,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotBlank @Size(min=3,max=8) String currency,
    @Size(max=128) String merchant,
    @Size(max=64) String country,
    @Size(max=64) String ipAddress,
    @Min(0) Integer recentTransactionCount10m,
    @Size(max=64) String homeCountry
) {}
