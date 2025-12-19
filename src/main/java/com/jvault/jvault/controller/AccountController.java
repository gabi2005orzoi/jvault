package com.jvault.jvault.controller;

import com.jvault.jvault.dto.AccountResponse;
import com.jvault.jvault.dto.CreateAccountRequest;
import com.jvault.jvault.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @RequestBody CreateAccountRequest request,
            Authentication authentication
    ){
        String userEmail = authentication.getName();
        return ResponseEntity.ok(accountService.createAccount(request, userEmail));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(Authentication authentication){
        return ResponseEntity.ok(accountService.getMyAccounts(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(
            @PathVariable Long id,
            Authentication authentication
    ){
        return ResponseEntity.ok(accountService.getAccountById(id, authentication.getName()));
    }
}
