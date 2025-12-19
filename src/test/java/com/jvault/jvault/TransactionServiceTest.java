package com.jvault.jvault;

import com.jvault.jvault.dto.DepositRequest;
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
import com.jvault.jvault.service.TransactionService;
import com.jvault.jvault.utils.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
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

    @InjectMocks
    private TransactionService transactionService;

    // Date de test reutilizabile
    private User user1;
    private User user2;
    private Account account1;
    private Account account2;

    @BeforeEach
    void setUp() {
        // IMPORTANT: Injectăm manual "self" pentru a simula comportamentul proxy-ului Spring
        // Astfel, când codul apelează self.saveFailedTransaction(), va apela instanța reală în test.
        ReflectionTestUtils.setField(transactionService, "self", transactionService);

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

    // --- TESTE PENTRU TRANSFER ---

    @Test
    @DisplayName("Transfer Success: Should move money and save transaction SUCCESS")
    void transferMoney_Success() {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO02VAULT002");
        request.setAmount(new BigDecimal("200.00"));
        request.setDescription("Rent payment");

        when(accountRepo.findById(1L)).thenReturn(Optional.of(account1));
        when(accountRepo.findByIban("RO02VAULT002")).thenReturn(Optional.of(account2));
        // Când se salvează tranzacția, o returnăm pe cea primită ca argument
        when(transactionRepo.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Transaction result = transactionService.transferMoney(request, "user1@test.com");

        // Assert
        assertNotNull(result);
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        assertEquals(TransactionType.TRANSFER, result.getType());
        assertEquals(new BigDecimal("200.00"), result.getAmount());

        // Verificăm balanțele actualizate
        assertEquals(new BigDecimal("800.00"), account1.getBalance());
        assertEquals(new BigDecimal("700.00"), account2.getBalance());

        // Verificăm interacțiunile cu repo
        verify(accountRepo).save(account1);
        verify(accountRepo).save(account2);
        verify(transactionRepo).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Transfer Fail: Insufficient Funds - Should throw exception AND save FAILED audit")
    void transferMoney_InsufficientFunds() {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO02VAULT002");
        request.setAmount(new BigDecimal("2000.00")); // Mai mult decât are (1000)

        when(accountRepo.findById(1L)).thenReturn(Optional.of(account1));
        when(accountRepo.findByIban("RO02VAULT002")).thenReturn(Optional.of(account2));

        // Act & Assert
        NotEnoughMoneyException exception = assertThrows(NotEnoughMoneyException.class, () ->
                transactionService.transferMoney(request, "user1@test.com")
        );

        assertEquals("Source account does not have enough money!", exception.getMessage());

        // Verificăm că banii NU s-au mutat
        assertEquals(new BigDecimal("1000.00"), account1.getBalance());
        assertEquals(new BigDecimal("500.00"), account2.getBalance());

        // CRITIC: Verificăm că s-a salvat tranzacția cu status FAILED
        // Asta testează logica try-catch-self din service
        verify(transactionRepo).save(argThat(transaction ->
                transaction.getStatus() == TransactionStatus.FAILED &&
                        transaction.getDescription().contains("Source account does not have enough money") &&
                        transaction.getSourceAccount().equals(account1)
        ));
    }

    @Test
    @DisplayName("Transfer Fail: Not Owner - Should throw exception AND save FAILED audit")
    void transferMoney_NotOwner() {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO02VAULT002");
        request.setAmount(new BigDecimal("50.00"));

        when(accountRepo.findById(1L)).thenReturn(Optional.of(account1));
        when(accountRepo.findByIban("RO02VAULT002")).thenReturn(Optional.of(account2));

        // Act & Assert
        // Încercăm să transferăm din account1 folosind email-ul user2
        NotYourAccountException exception = assertThrows(NotYourAccountException.class, () ->
                transactionService.transferMoney(request, "user2@test.com")
        );

        // Verificăm salvarea auditului FAILED
        verify(transactionRepo).save(argThat(t ->
                t.getStatus() == TransactionStatus.FAILED &&
                        t.getDescription().contains("You do not own this source account")
        ));
    }

    @Test
    @DisplayName("Transfer Fail: Same Account - Should throw exception AND save FAILED audit")
    void transferMoney_SameAccount() {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setSourceAccountId(1L);
        request.setDestinationIban("RO01VAULT001"); // Același IBAN ca sursa
        request.setAmount(new BigDecimal("50.00"));

        // Simulăm că findByIban returnează același cont ca findById
        when(accountRepo.findById(1L)).thenReturn(Optional.of(account1));
        when(accountRepo.findByIban("RO01VAULT001")).thenReturn(Optional.of(account1));

        // Act & Assert
        assertThrows(CantTransferMoneyToSameAccount.class, () ->
                transactionService.transferMoney(request, "user1@test.com")
        );

        verify(transactionRepo).save(argThat(t ->
                t.getStatus() == TransactionStatus.FAILED &&
                        t.getDescription().contains("Cannot transfer money to the same account")
        ));
    }

    @Test
    @DisplayName("Transfer Fail: Source Not Found - Should throw exception immediately")
    void transferMoney_SourceNotFound() {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setSourceAccountId(99L);
        when(accountRepo.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccountNotFoundException.class, () ->
                transactionService.transferMoney(request, "user1@test.com")
        );

        // Aici NU ajungem la saveFailedTransaction pentru că nu avem contul sursă
        verify(transactionRepo, never()).save(any());
    }

    // --- TESTE PENTRU DEPOSIT ---

    @Test
    @DisplayName("Deposit Success: Should add money")
    void deposit_Success() {
        // Arrange
        DepositRequest request = new DepositRequest("RO01VAULT001", new BigDecimal("500.00"));
        when(accountRepo.findByIban("RO01VAULT001")).thenReturn(Optional.of(account1));
        when(transactionRepo.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Transaction result = transactionService.deposit(request);

        // Assert
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        assertEquals(TransactionType.DEPOSIT, result.getType());
        assertEquals(new BigDecimal("1500.00"), account1.getBalance()); // 1000 + 500
        verify(accountRepo).save(account1);
    }

    // --- TESTE PENTRU WITHDRAWAL ---

    @Test
    @DisplayName("Withdrawal Success: Should subtract money")
    void withdrawal_Success() {
        // Arrange
        WithdrawalRequest request = new WithdrawalRequest("RO01VAULT001", new BigDecimal("100.00"));
        when(accountRepo.findByIban("RO01VAULT001")).thenReturn(Optional.of(account1));
        when(transactionRepo.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Transaction result = transactionService.withdrawal(request, "user1@test.com");

        // Assert
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        assertEquals(TransactionType.WITHDRAWAL, result.getType());
        assertEquals(new BigDecimal("900.00"), account1.getBalance()); // 1000 - 100
        verify(accountRepo).save(account1);
    }

    @Test
    @DisplayName("Withdrawal Fail: Insufficient Funds")
    void withdrawal_InsufficientFunds() {
        // Arrange
        WithdrawalRequest request = new WithdrawalRequest("RO01VAULT001", new BigDecimal("2000.00"));
        when(accountRepo.findByIban("RO01VAULT001")).thenReturn(Optional.of(account1));

        // Act & Assert
        assertThrows(NotEnoughMoneyException.class, () ->
                transactionService.withdrawal(request, "user1@test.com")
        );

        // Balanța nu trebuie să se schimbe
        assertEquals(new BigDecimal("1000.00"), account1.getBalance());
    }

    @Test
    @DisplayName("Withdrawal Fail: Not Owner")
    void withdrawal_NotOwner() {
        // Arrange
        WithdrawalRequest request = new WithdrawalRequest("RO01VAULT001", new BigDecimal("100.00"));
        when(accountRepo.findByIban("RO01VAULT001")).thenReturn(Optional.of(account1));

        // Act & Assert
        assertThrows(NotYourAccountException.class, () ->
                transactionService.withdrawal(request, "user2@test.com")
        );
    }

    // --- TESTE PENTRU ISTORIC ---

    @Test
    @DisplayName("History: Should return list of transactions for owner")
    void getTransactionHistory_Success() {
        // Arrange
        when(accountRepo.findById(1L)).thenReturn(Optional.of(account1));
        Transaction tx = Transaction.builder().id(100L).build();
        when(transactionRepo.findBySourceAccountOrDestinationAccountOrderByTimestampDesc(account1, account1))
                .thenReturn(Collections.singletonList(tx));

        // Act
        List<Transaction> result = transactionService.getTransactionHistory(1L, "user1@test.com");

        // Assert
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());
    }

    @Test
    @DisplayName("History Fail: Not Owner")
    void getTransactionHistory_NotOwner() {
        // Arrange
        when(accountRepo.findById(1L)).thenReturn(Optional.of(account1));

        // Act & Assert
        assertThrows(NotYourAccountException.class, () ->
                transactionService.getTransactionHistory(1L, "user2@test.com")
        );
    }
}