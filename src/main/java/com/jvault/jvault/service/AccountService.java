package com.jvault.jvault.service;

import com.jvault.jvault.dto.AccountResponse;
import com.jvault.jvault.dto.CreateAccountRequest;
import com.jvault.jvault.model.Account;
import com.jvault.jvault.model.User;
import com.jvault.jvault.model.emus.Currency;
import com.jvault.jvault.repo.AccountRepo;
import com.jvault.jvault.repo.UserRepo;
import com.jvault.jvault.utils.exception.AccountNotFoundException;
import com.jvault.jvault.utils.exception.InvalidCurrencyException;
import com.jvault.jvault.utils.exception.NotYourAccountException;
import com.jvault.jvault.utils.exception.UserNotFound;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepo accountRepo;
    private final UserRepo userRepo;
    private final AuditLogService auditLogService;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request, String userEmail){
        try{
            Currency currencyEnum = Currency.valueOf(request.getCurrency().toUpperCase());
            User user = userRepo.findByEmail(userEmail)
                    .orElseThrow(UserNotFound::new);
            Account account = Account.builder()
                    .user(user)
                    .iban(generateUniqueIban())
                    .currency(currencyEnum)
                    .balance(BigDecimal.ZERO)
                    .build();
            auditLogService.logAction(
                    userEmail,
                    "ACCOUNT_CREATED",
                    "New account created via API. IBAN: " + account.getIban() + ", Currency: " + account.getCurrency(),
                    null
            );
            return mapToResponse(accountRepo.save(account));
        }
        catch (IllegalArgumentException e){
            throw new InvalidCurrencyException("Invalid currency! Allowed: RON, EUR, USD");
        }
    }

    private String generateUniqueIban(){
        return "RO" + UUID.randomUUID().toString().replaceAll("-", " ").substring(0,20).toUpperCase();
    }

    private AccountResponse mapToResponse(Account account){
        return AccountResponse.builder()
                .id(account.getId())
                .iban(account.getIban())
                .currency(account.getCurrency().name())
                .balance(account.getBalance())
                .build();
    }

    public List<AccountResponse> getMyAccounts(String email){
        User user = userRepo.findByEmail(email).orElseThrow(UserNotFound::new);
        return accountRepo.findAllByUser(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public AccountResponse getAccountById(Long id, String email){
        Account account = accountRepo.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        if(!account.getUser().getEmail().equals(email)){
            throw new NotYourAccountException("Account does not belong to you");
        }
        return mapToResponse(account);
    }
}
