package com.jvault.jvault.controller;

import com.jvault.jvault.dto.CardResponse;
import com.jvault.jvault.dto.CreateCardRequest;
import com.jvault.jvault.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/card")
public class CardController {

    private final CardService cardService;

    @PostMapping
    public ResponseEntity<CardResponse> createCard(
            @RequestBody @Valid CreateCardRequest request,
            Authentication authentication
    ){
        return ResponseEntity.ok(cardService.createCard(request.getIban(), authentication.getName()));
    }

    @PatchMapping("/{cardNumber}/block")
    public ResponseEntity<String> blockCard(
            @PathVariable String cardNumber,
            Authentication authentication
    ){
        cardService.changeActivityStatus(cardNumber, authentication.getName(), false);
        return ResponseEntity.ok("The card was blocked successfully!");
    }

    @PatchMapping("/{cardNumber}/unblock")
    public ResponseEntity<String> unblockCard(
            @PathVariable String cardNumber,
            Authentication authentication
    ){
        cardService.changeActivityStatus(cardNumber, authentication.getName(), true);
        return ResponseEntity.ok("The card was unblocked successfully!");
    }
}
