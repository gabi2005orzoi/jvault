    package com.jvault.jvault.service;

    import com.jvault.jvault.dto.DepositRequest;
    import com.jvault.jvault.dto.TransactionResponse;
    import com.jvault.jvault.dto.TransferRequest;
    import com.jvault.jvault.dto.WithdrawalRequest;
    import com.jvault.jvault.model.Account;
    import com.jvault.jvault.model.Transaction;
    import com.jvault.jvault.model.emus.TransactionStatus;
    import com.jvault.jvault.model.emus.TransactionType;
    import com.jvault.jvault.repo.AccountRepo;
    import com.jvault.jvault.repo.TransactionRepo;
    import com.jvault.jvault.utils.exception.*;
    import jakarta.transaction.Transactional;
    import lombok.RequiredArgsConstructor;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.domain.Sort;
    import org.springframework.stereotype.Service;

    import java.time.LocalDateTime;

    @Service
    @RequiredArgsConstructor
    public class TransactionService {
        private final TransactionRepo transactionRepo;
        private final AccountRepo accountRepo;
        private final AuditTransactionService auditTransactionService;

        private TransactionResponse mapToResponse(Transaction transaction){
            return TransactionResponse.builder()
                    .id(transaction.getId())
                    .amount(transaction.getAmount())
                    .timestamp(transaction.getTimestamp())
                    .status(transaction.getStatus())
                    .type(transaction.getType())
                    .currency(transaction.getCurrency())
                    .description(transaction.getDescription())
                    .sourceAccountIban(transaction.getSourceAccount().getIban())
                    .destinationAccountIban(transaction.getDestinationAccount().getIban())
                    .build();
        }

        @Transactional
        public TransactionResponse transferMoney(TransferRequest request, String userEmail){
            Account source = accountRepo.findById(request.getSourceAccountId())
                    .orElseThrow(() -> new AccountNotFoundException("Source account not found"));

            Account destination = accountRepo.findByIban(request.getDestinationIban())
                    .orElseThrow(() -> new AccountNotFoundException("Destination not found"));

            try{
                validateTransfer(source, destination, request, userEmail);
            } catch (RuntimeException e){
                auditTransactionService.saveFailedTransaction(source, destination, request, e.getMessage());
                throw  e;
            }

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

            return mapToResponse(transactionRepo.save(transaction));
        }

        private void validateTransfer(Account source, Account destination, TransferRequest request, String userEmail){
            if(!source.getUser().getEmail().equals(userEmail))
                throw new NotYourAccountException("You do not own this source account!");
            if(source.getBalance().compareTo(request.getAmount()) < 0)
                throw new NotEnoughMoneyException("Source account does not have enough money!");
            if(source.getId().equals(destination.getId()))
                throw new CantTransferMoneyToSameAccount("Cannot transfer money to the same account!");
            if(!source.getCurrency().equals(destination.getCurrency()))
                throw new NotSupportedYetException("Cross-currency transfer not supported yet!");
        }

        public Page<TransactionResponse> getTransactionHistory(Long accountId, String userEmail, int page, int size) {
            Account account = accountRepo.findById(accountId)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found"));
            if(!account.getUser().getEmail().equals(userEmail))
                throw new NotYourAccountException("Access denied");

            Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());

            Page<Transaction> transactionPage = transactionRepo.findBySourceAccountOrDestinationAccount(account, account, pageable);

            return transactionPage.map(this::mapToResponse);
        }

        @Transactional
        public TransactionResponse deposit(DepositRequest request){
            Account targetAccount = accountRepo.findByIban(request.getTargetIban())
                    .orElseThrow(() -> new AccountNotFoundException("Account not found"));

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
            return mapToResponse(transactionRepo.save(transaction));
        }

        @Transactional
        public TransactionResponse withdrawal(WithdrawalRequest request, String userEmail){
            Account sourceAccount = accountRepo.findByIban(request.getSourceIban())
                    .orElseThrow(() -> new AccountNotFoundException("Account not found"));

            if(!sourceAccount.getUser().getEmail().equals(userEmail))
                throw new NotYourAccountException("You cannot withdraw money from an account you don't own!");

            if(sourceAccount.getBalance().compareTo(request.getAmount()) < 0)
                throw new NotEnoughMoneyException("Insufficient amount of money in this account!");

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

            return mapToResponse(transactionRepo.save(transaction));
        }
    }
