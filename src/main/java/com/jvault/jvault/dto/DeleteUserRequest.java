package com.jvault.jvault.dto;

import lombok.Data;

@Data
public class DeleteUserRequest {
    private String email;
    private String password;
}
