package com.jvault.jvault.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferRequest {
    @NotNull
    private Long sourceAccountId;

    @NotNull
    private String destinationIban;

    @NotNull
    @Min(value = 0, message = "Amount must be positive")
    private BigDecimal amount;

    private String description;
}
