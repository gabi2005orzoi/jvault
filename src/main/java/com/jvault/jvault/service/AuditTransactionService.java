package com.jvault.jvault.service;

import com.jvault.jvault.dto.TransferRequest;
import com.jvault.jvault.model.Account;
import com.jvault.jvault.model.Transaction;
import com.jvault.jvault.model.emus.TransactionStatus;
import com.jvault.jvault.model.emus.TransactionType;
import com.jvault.jvault.repo.TransactionRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditTransactionService {

    private final TransactionRepo transactionRepo;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void saveFailedTransaction(Account source, Account destination, TransferRequest request, String reason) {
        Transaction transaction = Transaction.builder()
                .sourceAccount(source)
                .destinationAccount(destination)
                .amount(request.getAmount())
                .currency(source.getCurrency())
                .timestamp(LocalDateTime.now())
                .status(TransactionStatus.FAILED)
                .type(TransactionType.TRANSFER)
                .description("FAILED: " + reason)
                .build();
        transactionRepo.save(transaction);
    }
}
