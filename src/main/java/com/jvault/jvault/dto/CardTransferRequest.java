package com.jvault.jvault.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardTransferRequest {

    @NotNull
    private String cardNumber;
    @NotNull
    private String cvv;
    @NotNull
    private LocalDate expirationDate;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private String iban;
}
