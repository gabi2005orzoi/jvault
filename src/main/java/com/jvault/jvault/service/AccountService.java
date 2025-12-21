package com.jvault.jvault.service;

import com.jvault.jvault.dto.AccountResponse;
import com.jvault.jvault.dto.CreateAccountRequest;
import com.jvault.jvault.model.Account;
import com.jvault.jvault.model.User;
import com.jvault.jvault.model.emus.Currency;
import com.jvault.jvault.repo.AccountRepo;
import com.jvault.jvault.repo.UserRepo;
import com.jvault.jvault.utils.IpUtils;
import com.jvault.jvault.utils.exception.AccountNotFoundException;
import com.jvault.jvault.utils.exception.InvalidCurrencyException;
import com.jvault.jvault.utils.exception.NotYourAccountException;
import com.jvault.jvault.utils.exception.UserNotFound;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepo accountRepo;
    private final UserRepo userRepo;
    private final AuditLogService auditLogService;

    private static final String COUNTRY_CODE = "RO";
    private static final String BANK_CODE = "JVLT";

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
                    IpUtils.getCurrentIp()
            );
            return mapToResponse(accountRepo.save(account));
        }
        catch (IllegalArgumentException e){
            throw new InvalidCurrencyException("Invalid currency! Allowed: RON, EUR, USD");
        }
    }

    private String generateUniqueIban(){

        String accountId = generateRandomDigits();

        String checkDigits = calculateCheckDigits(accountId);

        return COUNTRY_CODE + checkDigits + BANK_CODE + accountId;
    }

    private String calculateCheckDigits(String accountId) {
        String temp = BANK_CODE + accountId + COUNTRY_CODE + "00";
        StringBuilder numericString = new StringBuilder();
        for(char ch: temp.toCharArray()){
            if(Character.isDigit(ch)){
                numericString.append(ch);
            } else {
                numericString.append(Character.getNumericValue(ch));
            }
        }

        BigInteger bigInt = new BigInteger(numericString.toString());
        int remainder = bigInt.mod(BigInteger.valueOf(97)).intValue();

        int checkDigitValue = 98 - remainder;

        return (checkDigitValue < 10 ? "0" : "") + checkDigitValue;
    }

    private String generateRandomDigits() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(16);
        for(int i=0; i<16; i++)
            sb.append(random.nextInt(10));
        return sb.toString();
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
