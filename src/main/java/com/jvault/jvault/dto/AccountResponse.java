package com.jvault.jvault.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AccountResponse {
    private Long id;
    private String iban;
    private String currency;
    private BigDecimal balance;
}
