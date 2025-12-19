package com.jvault.jvault.service;

import com.jvault.jvault.dto.*;
import com.jvault.jvault.model.User;
import com.jvault.jvault.repo.UserRepo;
import com.jvault.jvault.service.mapper.UserMapper;
import com.jvault.jvault.utils.exception.InvalidPasswordDeleteException;
import com.jvault.jvault.utils.exception.OldPasswordIncorrect;
import com.jvault.jvault.utils.exception.UserAlreadyExistsException;
import com.jvault.jvault.utils.exception.UserNotFound;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;


import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService{

    private final UserRepo userRepo;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    private void mergeUser(User user, UpdateUserRequest request) {
        String details = "User ";
        if(StringUtils.isNotBlank(request.getAddress())) {
            user.setAddress(request.getAddress());
            details += "address ";
        }
        if(StringUtils.isNotBlank(request.getPhoneNumber())) {
            user.setPhoneNumber(request.getPhoneNumber());
            details += "phone number ";
        }
        details += "updated";
        auditLogService.logAction(
                user.getEmail(),
                "USER_UPDATED",
                details,
                null
        );
    }

    private boolean correctPassword(String rawPassword, String storedHash){
        return passwordEncoder.matches(rawPassword, storedHash);
    }

    @Transactional
    public UserResponse registerUser(UserRequest request){
        if(userRepo.findByEmail(request.getEmail()).isPresent()){
            throw new UserAlreadyExistsException();
        }
        User user = mapper.toUser(request);
        userRepo.save(user);
        auditLogService.logAction(
                request.getEmail(),
                "USER_REGISTERED",
                "User registered successfully",
                null
        );
        return mapper.fromUser(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        var user = userRepo.findById(id)
                .orElseThrow(UserNotFound::new);
        mergeUser(user, request);
        return mapper.fromUser(user);
    }

    @Transactional
    public UserResponse changePassword(ChangePasswordRequest request){
        User user = userRepo.findByEmail(request.getEmail()).orElseThrow(UserNotFound::new);
        if(!correctPassword(request.getOldPassword(), user.getPassword())){
            throw new OldPasswordIncorrect("Incorrect password!");
        }
        userRepo.save(user);
        auditLogService.logAction(
                user.getEmail(),
                "PASSWORD_CHANGED",
                "Password changed successfully",
                null
        );
        return mapper.fromUser(user);
    }

    public List<UserResponse> getAll() {
        return userRepo.findAll()
                .stream()
                .map(mapper::fromUser)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        return mapper.fromUser(userRepo.findById(id).orElseThrow(UserNotFound::new));
    }

    public UserResponse getUserByEmail(String email){
        return mapper.fromUser(userRepo.findByEmail(email).orElseThrow(UserNotFound::new));
    }

    @Transactional
    public void deleteUser(DeleteUserRequest request){
        User user = userRepo.findByEmail(request.getEmail()).orElseThrow(UserNotFound::new);
        if(correctPassword(request.getPassword(), user.getPassword())) {
            auditLogService.logAction(
                    request.getEmail(),
                    "USER_DELETED",
                    "User deleted successfully",
                    null
                    );
            userRepo.delete(user);
        } else {
            throw new InvalidPasswordDeleteException("Password incorrect!");
        }
    }
}
