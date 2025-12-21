package com.jvault.jvault.service.mapper;

import com.jvault.jvault.dto.UserRequest;
import com.jvault.jvault.dto.UserResponse;
import com.jvault.jvault.model.User;
import com.jvault.jvault.model.emus.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {
    private final PasswordEncoder passwordEncoder;

    public User toUser(UserRequest request){
        if(request == null)
            return null;
        return User.builder()
                .firstName(request.getFirstName())
                .secondName(request.getSecondName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .role(Role.USER)
                .build();
    }

    public UserResponse fromUser(User user){
        if (user == null)
            return null;
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .secondName(user.getSecondName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
