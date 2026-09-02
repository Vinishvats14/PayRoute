package com.vinu.payroute.auth.controller;

import com.vinu.payroute.auth.dto.AuthResponse;
import com.vinu.payroute.auth.dto.InternalVerifyPinRequest;
import com.vinu.payroute.auth.dto.LoginRequest;
import com.vinu.payroute.auth.dto.RegisterRequest;
import com.vinu.payroute.auth.dto.TransactionPinRequest;
import com.vinu.payroute.auth.dto.UserProfileResponse;
import com.vinu.payroute.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${internal.service-token:change-me}")
    private String internalServiceToken;

    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        return ResponseEntity.ok(
                authService.login(request)
        );
    }

    private Long requireUserId(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userId;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(authService.getProfile(requireUserId(userId)));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/transaction-pin")
    public ResponseEntity<Void> setTransactionPin(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TransactionPinRequest request
    ) {
        authService.setTransactionPin(requireUserId(userId), request.pin());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/internal/transaction-pin/verify")
    public ResponseEntity<Boolean> verifyTransactionPin(
            @RequestHeader(value = "X-Service-Token", required = false) String serviceToken,
            @Valid @RequestBody InternalVerifyPinRequest request
    ) {
        if (serviceToken == null || !internalServiceToken.equals(serviceToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid service token");
        }
        return ResponseEntity.ok(authService.verifyTransactionPin(request.userId(), request.pin()));
    }
}
