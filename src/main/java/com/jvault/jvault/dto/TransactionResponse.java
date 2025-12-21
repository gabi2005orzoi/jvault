package com.jvault.jvault.dto;

import com.jvault.jvault.model.emus.Currency;
import com.jvault.jvault.model.emus.TransactionStatus;
import com.jvault.jvault.model.emus.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponse {

    private Long id;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private TransactionStatus status;
    private TransactionType type;
    private Currency currency;
    private String description;
    private String sourceAccountIban;
    private String destinationAccountIban;
}
