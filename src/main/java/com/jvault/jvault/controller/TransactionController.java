package com.jvault.jvault.controller;

import com.jvault.jvault.dto.DepositRequest;
import com.jvault.jvault.dto.TransferRequest;
import com.jvault.jvault.dto.WithdrawalRequest;
import com.jvault.jvault.model.Transaction;
import com.jvault.jvault.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transaction")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<Transaction> transferMoney(
            @RequestBody TransferRequest request,
            Authentication authentication
    ){
        return ResponseEntity.ok(transactionService.transferMoney(request, authentication.getName()));
    }

    @GetMapping("/history/{accountId}")
    public ResponseEntity<Page<Transaction>> getHistory(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ){
        return ResponseEntity.ok(transactionService.getTransactionHistory(accountId, authentication.getName(), page, size));
    }

    @PostMapping("/deposit")
    public ResponseEntity<Transaction> deposit(
            @RequestBody DepositRequest depositRequest
    ){
        return ResponseEntity.ok(transactionService.deposit(depositRequest));
    }

    @PostMapping("/withdrawal")
    public ResponseEntity<Transaction> withdrawal(
            @RequestBody WithdrawalRequest request,
            Authentication authentication
    ){
        return  ResponseEntity.ok(transactionService.withdrawal(request, authentication.getName()));
    }
}
