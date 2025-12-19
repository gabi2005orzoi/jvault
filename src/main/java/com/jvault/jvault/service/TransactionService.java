package com.jvault.jvault.service;

import com.jvault.jvault.dto.DepositRequest;
import com.jvault.jvault.dto.TransferRequest;
import com.jvault.jvault.dto.WithdrawalRequest;
import com.jvault.jvault.model.Account;
import com.jvault.jvault.model.Transaction;
import com.jvault.jvault.model.emus.TransactionStatus;
import com.jvault.jvault.model.emus.TransactionType;
import com.jvault.jvault.repo.AccountRepo;
import com.jvault.jvault.repo.TransactionRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepo transactionRepo;
    private final AccountRepo accountRepo;

    @Transactional
    public Transaction transferMoney(TransferRequest request, String userEmail){
        Account source = accountRepo.findById(request.getSourceAccountId())
                .orElseThrow(() -> new RuntimeException("Source account not found"));

        if(!source.getUser().getEmail().equals(userEmail))
            throw new RuntimeException("You do not own this source account!");

        if(source.getBalance().compareTo(request.getAmount()) <0)
            throw new RuntimeException("Source account does not have enough money!");

        Account destination = accountRepo.findByIban(request.getDestinationIban())
                .orElseThrow(() -> new RuntimeException("Destination not found"));

        if(source.getId().equals(destination.getId()))
            throw new RuntimeException("Cannot transfer money to the same account!");

        if(!source.getCurrency().equals(destination.getCurrency()))
            throw new RuntimeException("Cross-currency transfer not supported yet");

        source.setBalance(source.getBalance().subtract(request.getAmount()));
        destination.setBalance(destination.getBalance().add(request.getAmount()));

        accountRepo.save(source);
        accountRepo.save(destination);

        Transaction transaction = Transaction.builder()
                .sourceAccount(source)
                .destinationAccount(destination)
                .amount(request.getAmount())
                .currency(source.getCurrency())
                .timestamp(LocalDateTime.now())
                .status(TransactionStatus.SUCCESS)
                .type(TransactionType.TRANSFER)
                .description(request.getDescription())
                .build();

        return transactionRepo.save(transaction);
    }

    public List<Transaction> getTransactionHistory(Long accountId, String userEmail) {
        Account account = accountRepo.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        if(!account.getUser().getEmail().equals(userEmail))
            throw new RuntimeException("Access denied");

        return transactionRepo.findBySourceAccountOrDestinationAccountOrderByTimestampDesc(account, account);
    }

    @Transactional
    public Transaction deposit(DepositRequest request){
        Account targetAccount = accountRepo.findByIban(request.getTargetIban())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        targetAccount.setBalance(targetAccount.getBalance().add(request.getAmount()));
        accountRepo.save(targetAccount);

        Transaction transaction = Transaction.builder()
                .sourceAccount(null)
                .destinationAccount(targetAccount)
                .amount(request.getAmount())
                .currency(targetAccount.getCurrency())
                .timestamp(LocalDateTime.now())
                .status(TransactionStatus.SUCCESS)
                .type(TransactionType.DEPOSIT)
                .build();
        return transactionRepo.save(transaction);
    }

    @Transactional
    public Transaction withdrawal(WithdrawalRequest request, String userEmail){
        Account sourceAccount = accountRepo.findByIban(request.getSourceIban())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if(!sourceAccount.getUser().getEmail().equals(userEmail))
            throw new RuntimeException("You cannot withdraw money from an account you don't own!");

        if(sourceAccount.getBalance().compareTo(request.getAmount()) < 0)
            throw new RuntimeException("Insufficient amount of money in this account!");

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.getAmount()));
        accountRepo.save(sourceAccount);

        Transaction transaction = Transaction.builder()
                .sourceAccount(sourceAccount)
                .destinationAccount(null)
                .amount(request.getAmount())
                .currency(sourceAccount.getCurrency())
                .timestamp(LocalDateTime.now())
                .status(TransactionStatus.SUCCESS)
                .type(TransactionType.WITHDRAWAL)
                .build();

        return transactionRepo.save(transaction);
    }
}
