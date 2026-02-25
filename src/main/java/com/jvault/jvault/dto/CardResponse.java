package com.jvault.jvault.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CardResponse {
    private String cardNumber;
    private String cvv;
    private LocalDate expirationDate;
    private boolean isActive;
}
