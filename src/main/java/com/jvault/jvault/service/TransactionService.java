    package com.jvault.jvault.service;

    import com.jvault.jvault.dto.*;
    import com.jvault.jvault.model.Account;
    import com.jvault.jvault.model.Card;
    import com.jvault.jvault.model.Transaction;
    import com.jvault.jvault.model.emus.TransactionStatus;
    import com.jvault.jvault.model.emus.TransactionType;
    import com.jvault.jvault.repo.AccountRepo;
    import com.jvault.jvault.repo.CardRepo;
    import com.jvault.jvault.repo.TransactionRepo;
    import com.jvault.jvault.utils.exception.*;
    import jakarta.annotation.PostConstruct;
    import jakarta.transaction.Transactional;
    import lombok.RequiredArgsConstructor;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.domain.Sort;
    import org.springframework.messaging.simp.SimpMessagingTemplate;
    import org.springframework.scheduling.annotation.Scheduled;
    import org.springframework.stereotype.Service;
    import org.springframework.web.reactive.function.client.WebClient;

    import java.math.BigDecimal;
    import java.math.RoundingMode;
    import java.time.Duration;
    import java.time.LocalDate;
    import java.time.LocalDateTime;
    import java.util.Map;
    import java.util.concurrent.ConcurrentHashMap;

    @Service
    @RequiredArgsConstructor
    public class TransactionService {
        private final TransactionRepo transactionRepo;
        private final AccountRepo accountRepo;
        private final AuditTransactionService auditTransactionService;
        private final CardRepo cardRepo;
        private final SimpMessagingTemplate messagingTemplate;

        @Value("${application.security.exchange_rate.key}")
        private String exrKey;
        private final Map<String, BigDecimal> rates = new ConcurrentHashMap<>();

        private TransactionResponse mapToResponse(Transaction transaction){
            return TransactionResponse.builder()
                    .id(transaction.getId())
                    .amount(transaction.getAmount())
                    .timestamp(transaction.getTimestamp())
                    .status(transaction.getStatus())
                    .type(transaction.getType())
                    .currency(transaction.getCurrency())
                    .description(transaction.getDescription())
                    .sourceAccountIban(transaction.getSourceAccount() != null ? transaction.getSourceAccount().getIban() : null)
                    .destinationAccountIban(transaction.getDestinationAccount() != null ? transaction.getDestinationAccount().getIban() : null)
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
            BigDecimal convertedAmount = convert(request.getAmount(), source.getCurrency().name(), destination.getCurrency().name());
            destination.setBalance(destination.getBalance().add(convertedAmount));

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

            TransactionResponse response = mapToResponse(transactionRepo.save(transaction));

            String destinationUserEmail = destination.getUser().getEmail();
            messagingTemplate.convertAndSend("/topic/notifications/" + destinationUserEmail, response);
            messagingTemplate.convertAndSend("/topic/notifications/" + userEmail, response);
            return response;
        }

        private void validateTransfer(Account source, Account destination, TransferRequest request, String userEmail){
            if(!source.getUser().getEmail().equals(userEmail))
                throw new NotYourAccountException("You do not own this source account!");
            if(source.getBalance().compareTo(request.getAmount()) < 0)
                throw new NotEnoughMoneyException("Source account does not have enough money!");
            if(source.getId().equals(destination.getId()))
                throw new CantTransferMoneyToSameAccount("Cannot transfer money to the same account!");
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

            TransactionResponse response = mapToResponse(transactionRepo.save(transaction));
            String destinationUserEmail = targetAccount.getUser().getEmail();
            messagingTemplate.convertAndSend("/topic/notifications/" + destinationUserEmail, response);
            return response;
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

            TransactionResponse response = mapToResponse(transactionRepo.save(transaction));
            messagingTemplate.convertAndSend("/topic/notifications/" + userEmail, response);
            return response;
        }

        @Transactional
        public TransactionResponse cardTransaction(CardTransferRequest request){
            Card card = cardRepo.findByCardNumber(request.getCardNumber()).orElseThrow(() -> new CardException("Card not found!"));
            if(!card.getCvv().equals(request.getCvv()))
                throw new CardException("Invalid card data");
            if(!card.isActive())
                throw new CardException("Card blocked");
            if(card.getExpirationDate().isBefore(LocalDate.now()))
                throw new CardException("Card expired");
            if(!card.getExpirationDate().equals(request.getExpirationDate()))
                throw new CardException("Invalid card data");
            if(request.getAmount().compareTo(BigDecimal.valueOf(0))<0)
                throw new NegativeAmountException("Cannot transfer negative amounts of money!");
            if(card.getAccount().getBalance().compareTo(request.getAmount())<0)
                throw new NotEnoughMoneyException("Insufficient amount of money in this account!");

            card.getAccount().setBalance(
                    card.getAccount().getBalance().subtract(request.getAmount())
            );
            Account destination = null;

            if(request.getIban() != null && request.getIban().contains("JVLT")){
                destination = accountRepo.findByIban(request.getIban()).orElseThrow(() -> new AccountNotFoundException("Account not found"));
                BigDecimal amountForDestination = convert(request.getAmount(), card.getAccount().getCurrency().name(), card.getAccount().getCurrency().name());
                destination.setBalance(destination.getBalance().add(amountForDestination));
            }

            accountRepo.save(card.getAccount());

            if(destination!=null){
                accountRepo.save(destination);
            }

            Transaction transaction = Transaction.builder()
                    .sourceAccount(card.getAccount())
                    .destinationAccount(destination)
                    .amount(request.getAmount())
                    .currency(card.getAccount().getCurrency())
                    .timestamp(LocalDateTime.now())
                    .description("Card payment to: " + request.getIban())
                    .status(TransactionStatus.SUCCESS)
                    .type(TransactionType.CARD_PAYMENT)
                    .build();

            TransactionResponse response = mapToResponse(transactionRepo.save(transaction));
            String sourceUserEmail = card.getAccount().getUser().getEmail();
            messagingTemplate.convertAndSend("/topic/notifications/" + sourceUserEmail, response);

            if(destination!=null) {
                String destinationUserEmail = destination.getUser().getEmail();
                messagingTemplate.convertAndSend("/topic/notifications/" + destinationUserEmail, response);
            }
            return response;
        }

        @Scheduled(fixedRate = 1800000)
        @PostConstruct
        public void updateExchangeRate(){
            final WebClient client = WebClient.builder()
                    .baseUrl("https://v6.exchangerate-api.com/v6/" + exrKey + "/latest/USD")
                    .build();
            try{
                ExchangeRate response = client.get()
                        .uri("https://v6.exchangerate-api.com/v6/" + exrKey + "/latest/USD")
                        .retrieve()
                        .bodyToMono(ExchangeRate.class)
                        .block(Duration.ofSeconds(10));
                if(response!=null && response.getConversionRate()!=null){
                    this.rates.putAll(response.getConversionRate());
                }
            } catch (Exception e){
                throw new UpdateCurrencyException("Could not update the currency");
            }
        }

        public BigDecimal getRate(String currency){
            return rates.getOrDefault(currency.toUpperCase(), BigDecimal.valueOf(1));
        }

        private BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency){
            if(fromCurrency.equals(toCurrency))
                return amount;
            BigDecimal rateFrom = getRate(fromCurrency);
            BigDecimal rateTo = getRate(toCurrency);

            return amount.divide(rateFrom, 10, RoundingMode.HALF_UP)
                    .multiply(rateTo)
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }
