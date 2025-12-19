package com.jvault.jvault.repo;

import com.jvault.jvault.model.Account;
import com.jvault.jvault.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepo extends JpaRepository<Transaction, Long> {
    List<Transaction> findBySourceAccountOrDestinationAccountOrderByTimestampDesc(Account sourceAccount, Account destinationAccount);
}
