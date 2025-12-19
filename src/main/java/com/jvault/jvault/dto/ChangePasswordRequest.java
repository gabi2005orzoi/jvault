package com.jvault.jvault.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email;

    @NotBlank(message = "Old password is required")
    @Size(min = 8, message = "Password must be al least 8 characters long")
    String oldPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be al least 8 characters long")
    String newPassword;
}
