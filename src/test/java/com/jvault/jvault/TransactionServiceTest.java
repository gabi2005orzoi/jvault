package com.jvault.jvault;

import com.jvault.jvault.dto.DepositRequest;
import com.jvault.jvault.dto.TransactionResponse;
import com.jvault.jvault.dto.TransferRequest;
import com.jvault.jvault.dto.WithdrawalRequest;
import com.jvault.jvault.model.Account;
import com.jvault.jvault.model.Transaction;
import com.jvault.jvault.model.User;
import com.jvault.jvault.model.emus.Currency;
import com.jvault.jvault.model.emus.TransactionStatus;
import com.jvault.jvault.model.emus.TransactionType;
import com.jvault.jvault.repo.AccountRepo;
import com.jvault.jvault.repo.TransactionRepo;
import com.jvault.jvault.service.AuditTransactionService;
import com.jvault.jvault.service.TransactionService;
import com.jvault.jvault.utils.exception.CantTransferMoneyToSameAccount;
import com.jvault.jvault.utils.exception.NotEnoughMoneyException;
import com.jvault.jvault.utils.exception.NotYourAccountException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepo transactionRepo;
    @Mock
    private AccountRepo accountRepo;
    @Mock
    private AuditTransactionService auditTransactionService;

    @InjectMocks
    private TransactionService transactionService;

    private User user1;
    private User user2;
    private Account account1;
    private Account account2;

    @BeforeEach
    void setUp() {
        user1 = User.builder().id(1L).email("user1@test.com").build();
        user2 = User.builder().id(2L).email("user2@test.com").build();

        account1 = Account.builder()
                .id(1L)
                .iban("RO01VAULT001")
                .balance(new BigDecimal("1000.00"))
                .currency(Currency.RON)
                .user(user1)
                .build();

        account2 = Account.builder()
                .id(2L)
                .iban("RO02VAULT002")
                .balance(new BigDecimal("500.00"))
                .currency(Currency.RON)
                .user(user2)
                .build();
    }


    @Test
    @DisplayName("Transfer Success: Should move money and return Response DTO")
    void transferMoney_Success() {
        TransferRequest request = new TransferRequest();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO02VAULT002");
        request.setAmount(new BigDecimal("200.00"));
        request.setDescription("Rent payment");

        when(accountRepo.findById(1L)).thenReturn(Optional.of(account1));
        when(accountRepo.findByIban("RO02VAULT002")).thenReturn(Optional.of(account2));

        when(transactionRepo.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(100L);
            return t;
        });

        TransactionResponse result = transactionService.transferMoney(request, "user1@test.com");

        assertNotNull(result);
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        assertEquals(TransactionType.TRANSFER, result.getType());
        assertEquals(new BigDecimal("200.00"), result.getAmount());
        assertEquals("RO01VAULT001", result.getSourceAccountIban());
        assertEquals("RO02VAULT002", result.getDestinationAccountIban());

        assertEquals(new BigDecimal("800.00"), account1.getBalance());
        assertEquals(new BigDecimal("700.00"), account2.getBalance());

        verify(auditTransactionService, never()).saveFailedTransaction(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Transfer Fail: Insufficient Funds - Should call AuditTransactionService")
    void transferMoney_InsufficientFunds() {
        TransferRequest request = new TransferRequest();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO02VAULT002");
        request.setAmount(new BigDecimal("2000.00")); // Mai mult decât are (1000)

        when(accountRepo.findById(1L)).thenReturn(Optional.of(account1));
        when(accountRepo.findByIban("RO02VAULT002")).thenReturn(Optional.of(account2));

        NotEnoughMoneyException exception = assertThrows(NotEnoughMoneyException.class, () ->
                transactionService.transferMoney(request, "user1@test.com")
        );

        assertEquals("Source account does not have enough money!", exception.getMessage());

        assertEquals(new BigDecimal("1000.00"), account1.getBalance());

        verify(auditTransactionService).saveFailedTransaction(
                eq(account1),
                eq(account2),
                eq(request),
                contains("Source account does not have enough money")
        );
    }

    @Test
    @DisplayName("Transfer Fail: Not Owner")
    void transferMoney_NotOwner() {
        TransferRequest request = new TransferRequest(1L, "RO02VAULT002", new BigDecimal("50.00"), "test");

        when(accountRepo.findById(1L)).thenReturn(Optional.of(account1));
        when(accountRepo.findByIban("RO02VAULT002")).thenReturn(Optional.of(account2));

        assertThrows(NotYourAccountException.class, () ->
                transactionService.transferMoney(request, "user2@test.com")
        );

        verify(auditTransactionService).saveFailedTransaction(any(), any(), any(), contains("You do not own this source account"));
    }

    @Test
    @DisplayName("Transfer Fail: Same Account")
    void transferMoney_SameAccount() {
        TransferRequest request = new TransferRequest(1L, "RO01VAULT001", new BigDecimal("50.00"), "test");

        when(accountRepo.findById(1L)).thenReturn(Optional.of(account1));
        when(accountRepo.findByIban("RO01VAULT001")).thenReturn(Optional.of(account1));

        assertThrows(CantTransferMoneyToSameAccount.class, () ->
                transactionService.transferMoney(request, "user1@test.com")
        );

        verify(auditTransactionService).saveFailedTransaction(any(), any(), any(), contains("Cannot transfer money to the same account"));
    }


    @Test
    @DisplayName("Deposit Success: Should add money and handle NULL source account in mapper")
    void deposit_Success() {
        DepositRequest request = new DepositRequest("RO01VAULT001", new BigDecimal("500.00"));
        when(accountRepo.findByIban("RO01VAULT001")).thenReturn(Optional.of(account1));

        when(transactionRepo.save(any(Transaction.class))).thenAnswer(i -> {
            Transaction t = i.getArgument(0);
            t.setId(200L);
            return t;
        });

        TransactionResponse result = transactionService.deposit(request);

        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        assertEquals(TransactionType.DEPOSIT, result.getType());
        assertNull(result.getSourceAccountIban()); // Aici verificăm fix-ul pentru NPE
        assertEquals("RO01VAULT001", result.getDestinationAccountIban());

        assertEquals(new BigDecimal("1500.00"), account1.getBalance());
    }


    @Test
    @DisplayName("Withdrawal Success: Should subtract money and handle NULL destination in mapper")
    void withdrawal_Success() {
        WithdrawalRequest request = new WithdrawalRequest("RO01VAULT001", new BigDecimal("100.00"));
        when(accountRepo.findByIban("RO01VAULT001")).thenReturn(Optional.of(account1));

        when(transactionRepo.save(any(Transaction.class))).thenAnswer(i -> {
            Transaction t = i.getArgument(0);
            t.setId(300L);
            return t;
        });

        TransactionResponse result = transactionService.withdrawal(request, "user1@test.com");

        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        assertEquals(TransactionType.WITHDRAWAL, result.getType());
        assertEquals("RO01VAULT001", result.getSourceAccountIban());
        assertNull(result.getDestinationAccountIban()); // Aici verificăm fix-ul pentru NPE

        assertEquals(new BigDecimal("900.00"), account1.getBalance());
    }

    @Test
    @DisplayName("History: Should map Transaction Entity to Response DTO")
    void getTransactionHistory_Success() {
        when(accountRepo.findById(1L)).thenReturn(Optional.of(account1));

        Transaction tx = Transaction.builder()
                .id(100L)
                .amount(new BigDecimal("50.00"))
                .sourceAccount(account1)
                .destinationAccount(account2)
                .timestamp(LocalDateTime.now())
                .build();

        Page<Transaction> pageResult = new PageImpl<>(Collections.singletonList(tx));

        when(transactionRepo.findBySourceAccountOrDestinationAccount(
                eq(account1),
                eq(account1),
                any(Pageable.class))
        ).thenReturn(pageResult);

        Page<TransactionResponse> result = transactionService.getTransactionHistory(1L, "user1@test.com", 0, 10);

        assertEquals(1, result.getTotalElements());
        TransactionResponse responseDto = result.getContent().get(0);

        assertEquals(100L, responseDto.getId());
        assertEquals("RO01VAULT001", responseDto.getSourceAccountIban());
        assertEquals("RO02VAULT002", responseDto.getDestinationAccountIban());
    }
}