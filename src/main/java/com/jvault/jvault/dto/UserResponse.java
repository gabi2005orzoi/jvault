package com.jvault.jvault.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String firstName;
    private String secondName;
    private String email;
    private String role;
    private List<String> accountIbans;
}
