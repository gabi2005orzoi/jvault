package com.jvault.jvault.controller;

import com.jvault.jvault.dto.*;
import com.jvault.jvault.model.RefreshToken;
import com.jvault.jvault.service.AuditLogService;
import com.jvault.jvault.service.JwtService;
import com.jvault.jvault.service.RefreshTokenService;
import com.jvault.jvault.service.UserService;
import com.jvault.jvault.utils.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final AuditLogService auditLogService;

    private boolean isOwnerOrAdmin(String resourceOwnerEmail, Authentication authentication){
        String currentUsername = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"));
        return currentUsername.equals(resourceOwnerEmail) || isAdmin;
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> authenticateAndGetToken(
            @RequestBody AuthRequest authRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
        );
        if(authentication.isAuthenticated()){
            String accessToken = jwtService.generateToken(authRequest.getUsername());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(authRequest.getUsername());

            ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken.getToken())
                    .httpOnly(true)
                    .secure(false)
                    .maxAge(7*24*60*60)
                    .sameSite("Strict")
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

            String ipAddress = IpUtils.getIpClient(request);
            auditLogService.logAction(
                    authRequest.getUsername(),
                    "LOGIN_SUCCESS",
                    "User logged in successfully",
                    ipAddress
            );
            return ResponseEntity.ok(JwtResponse.builder()
                    .accessToken(accessToken)
                    .build()
            );
        } else {
            throw new UsernameNotFoundException("Invalid user request");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response){
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken","")
                .httpOnly(true)
                .secure(false)
                .path("api/user/refresh-token")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
        return ResponseEntity.ok("Logged out successfully!");
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<JwtResponse> refreshToken(
            @CookieValue(name = "refreshToken") String refreshToken
    ){
        return refreshTokenService.findByToken(refreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String accessToken = jwtService.generateToken(user.getEmail());
                    return ResponseEntity.ok(JwtResponse.builder()
                            .accessToken(accessToken)
                            .build());
                }).orElseThrow(() -> new RuntimeException("Refresh token is not in the database"));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(
            @RequestBody @Valid UserRequest request
    ){
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerUser(request));
    }

    @PostMapping("/delete-account")
    public ResponseEntity<String> deleteUser(
            @RequestBody DeleteUserRequest request,
            Principal principal
    ){
        if(!request.getEmail().equals(principal.getName()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only delete your own account");
        userService.deleteUser(request);
        return ResponseEntity.ok("Account deleted successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @RequestBody @Valid UpdateUserRequest request,
            @PathVariable Long id,
            Authentication authentication // we use authentication to verify the roles
    ){
        UserResponse existingUser = userService.getUserById(id);
        if(!isOwnerOrAdmin(existingUser.getEmail(), authentication))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PutMapping("/change-password")
    public ResponseEntity<UserResponse> changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ){
        if(!isOwnerOrAdmin(request.getEmail(), authentication))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(userService.changePassword(request));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll(){
        return ResponseEntity.ok(userService.getAll());
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id,
            Authentication authentication
    ){
        UserResponse userResponse = userService.getUserById(id);
        if(!isOwnerOrAdmin(userResponse.getEmail(), authentication))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(userResponse);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(
            @PathVariable String email,
            Authentication authentication
    ){
        UserResponse userResponse = userService.getUserByEmail(email);
        if(!isOwnerOrAdmin(userResponse.getEmail(), authentication))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

}
