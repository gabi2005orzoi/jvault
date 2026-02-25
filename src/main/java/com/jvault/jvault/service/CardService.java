package com.jvault.jvault.service;

import com.jvault.jvault.dto.CardResponse;
import com.jvault.jvault.model.Account;
import com.jvault.jvault.model.Card;
import com.jvault.jvault.model.User;
import com.jvault.jvault.model.emus.Role;
import com.jvault.jvault.repo.AccountRepo;
import com.jvault.jvault.repo.CardRepo;
import com.jvault.jvault.repo.UserRepo;
import com.jvault.jvault.utils.exception.AccountNotFoundException;
import com.jvault.jvault.utils.exception.CardException;
import com.jvault.jvault.utils.exception.NotYourAccountException;
import com.jvault.jvault.utils.exception.UserNotFound;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepo cardRepo;
    private final AccountRepo accountRepo;
    private final UserRepo userRepo;

    @Transactional
    public CardResponse createCard(String iban, String email){
        Account account = accountRepo.findByIban(iban).orElseThrow(() -> new AccountNotFoundException("Account not found!"));

        if(!account.getUser().getEmail().equals(email))
            throw new NotYourAccountException("This is not your account and you are not allowed to create a card!");

        Card card = Card.builder()
                .cardNumber(generateCardNumber())
                .cvv(generateNumberStr(3))
                .expirationDate(LocalDate.now().plusYears(5))
                .isActive(true)
                .account(account)
                .build();

        cardRepo.save(card);
        var cards = account.getCards();
        cards.add(card);
        account.setCards(cards);
        accountRepo.save(account);

        return mapToResponse(card);
    }

    private CardResponse mapToResponse(Card card){
        return CardResponse.builder()
                .cardNumber(card.getCardNumber())
                .cvv(card.getCvv())
                .expirationDate(card.getExpirationDate())
                .isActive(card.isActive())
                .build();
    }

    private String generateNumberStr(int len){
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<len; i++)
            sb.append(random.nextInt(10));
        return sb.toString();
    }

    private String generateCardNumber(){
        while (true){
            StringBuilder number = new StringBuilder("410293");
            String unique = generateNumberStr(9);
            number.append(unique);
            int check = checksum(number.toString());
            number.append(check);
            String candidateCardNumber = number.toString();

            if(!cardRepo.existsCardByCardNumber(candidateCardNumber))
                return candidateCardNumber;
        }
    }

    private int checksum(String n){
        // Luhn algorithm
        char[] c = n.toCharArray();
        int d=1;
        int sum=0;
        for(int i = c.length-1; i>=0; i--){
            int e = (d%2+1)*(c[i]-'0');
            if(e>=10){
                sum += e/10 + e%10;
            } else sum+= e;
            d++;
        }
        return (10-sum%10)%10;
    }

    @Transactional
    public void changeActivityStatus(String cardNumber, String userWhoRequest, boolean activity){
        Card card = cardRepo.findByCardNumber(cardNumber).orElseThrow(() -> new CardException("This card doesn't exist"));
        User currentUser = userRepo.findByEmail(userWhoRequest)
                .orElseThrow(UserNotFound::new);
        if (!card.getAccount().getUser().getEmail().equals(userWhoRequest) && !currentUser.getRole().equals(Role.ADMIN))
            throw new NotYourAccountException("You don't have the permission to modify the status of this card");
        card.setActive(activity);
    }
}
