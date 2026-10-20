package com.demo.escape_game_booking.auth;

import com.demo.escape_game_booking.auth.dto.LoginRequest;
import com.demo.escape_game_booking.auth.dto.RegisterRequest;
import com.demo.escape_game_booking.auth.dto.TokenResponse;
import com.demo.escape_game_booking.auth.dto.UserResponse;
import com.demo.escape_game_booking.common.ConflictException;
import com.demo.escape_game_booking.user.Role;
import com.demo.escape_game_booking.user.User;
import com.demo.escape_game_booking.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@Transactional
public class AuthService {

    private static final String EMAIL_TAKEN = "Un compte existe déjà avec cet email.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final Clock clock;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       TokenService tokenService, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.clock = clock;
    }

    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException(EMAIL_TAKEN);
        }

        User user = new User(email, passwordEncoder.encode(request.password()),
                request.firstName().trim(), request.lastName().trim(),
                Role.CUSTOMER, LocalDateTime.now(clock));

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Deux inscriptions simultanées avec le même email
            throw new ConflictException(EMAIL_TAKEN);
        }

        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        return tokenService.createToken(user);
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}