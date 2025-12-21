package com.jvault.jvault.repo;

import com.jvault.jvault.model.Account;
import com.jvault.jvault.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepo extends JpaRepository<Transaction, Long> {
    Page<Transaction> findBySourceAccountOrDestinationAccount(
            Account sourceAccount,
            Account destinationAccount,
            Pageable pageable
    );
}
