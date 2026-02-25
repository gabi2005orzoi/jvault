package com.jvault.jvault.repo;

import com.jvault.jvault.model.Card;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardRepo extends JpaRepository<Card, Long> {
    Optional<Card> findByCardNumber(@NotNull String cardNumber);

    boolean existsCardByCardNumber(String cardNumber);
}
