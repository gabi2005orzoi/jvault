package com.jvault.jvault.repo;

import com.jvault.jvault.model.Account;
import com.jvault.jvault.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepo extends JpaRepository<Account, Long> {
    List<Account> findAllByUser(User user);

    Optional<Account> findByIban(String iban);
}
