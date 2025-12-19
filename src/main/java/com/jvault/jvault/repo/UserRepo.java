package com.jvault.jvault.repo;

import com.jvault.jvault.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Long> {
    Optional<User> findByEmail(@NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email);
}
