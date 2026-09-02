package com.vinu.payroute.auth.service;

import com.vinu.payroute.auth.entity.Role;
import com.vinu.payroute.auth.entity.User;
import com.vinu.payroute.auth.repository.UserRepository;
import com.vinu.payroute.auth.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldSetTransactionPinForAuthenticatedUser() {
        User user = User.builder()
                .id(7L)
                .email("demo@example.com")
                .password("hashed")
                .role(Role.USER)
                .active(true)
                .build();

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        authService.setTransactionPin(7L, "1234");

        assertEquals("1234", user.getTransactionPin());
    }
}
