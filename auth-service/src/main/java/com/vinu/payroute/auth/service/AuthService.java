package com.vinu.payroute.auth.service;

import com.vinu.payroute.auth.dto.AuthResponse;
import com.vinu.payroute.auth.dto.LoginRequest;
import com.vinu.payroute.auth.dto.RegisterRequest;
import com.vinu.payroute.auth.dto.UserProfileResponse;
import com.vinu.payroute.auth.entity.Role;
import com.vinu.payroute.auth.entity.User;
import com.vinu.payroute.auth.repository.UserRepository;
import com.vinu.payroute.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public void register(RegisterRequest  registerRequest) {

        String email = registerRequest.email().toLowerCase().trim();
        if(userRepository.findByEmail(email).isPresent()){
            throw new IllegalArgumentException("Email already registered");
        }
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(registerRequest.password()))
                .role(Role.USER)
                .active(true)
                .build();

        userRepository.save(user);
    }

    @Transactional
    public void setTransactionPin(Long userId, String transactionPin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String normalized = transactionPin == null ? null : transactionPin.trim();
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException("Transaction PIN is required");
        }
        if (!normalized.matches("\\d{4,6}")) {
            throw new IllegalArgumentException("Transaction PIN must be 4 to 6 digits");
        }

        user.setTransactionPin(passwordEncoder.encode(normalized));
        userRepository.save(user);
    }

    public boolean verifyTransactionPin(Long userId, String transactionPin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getTransactionPin() == null || transactionPin == null) {
            return false;
        }

        return passwordEncoder.matches(transactionPin.trim(), user.getTransactionPin());
    }

    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getTransactionPin() != null && !user.getTransactionPin().isBlank()
        );
    }

    public AuthResponse login(LoginRequest request) {

        String email = request.email().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid email or password")
                );

        if (!user.getActive()) {
            throw new BadCredentialsException("User account is inactive");
        }

        if (!passwordEncoder.matches(
                request.password(),
                user.getPassword()
        )) {
            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        String token = jwtService.generateToken(user);

        return new AuthResponse(token, "Bearer");
    }
}