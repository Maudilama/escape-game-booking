package com.demo.escape_game_booking.auth;

import com.demo.escape_game_booking.auth.dto.LoginRequest;
import com.demo.escape_game_booking.auth.dto.RegisterRequest;
import com.demo.escape_game_booking.auth.dto.TokenResponse;
import com.demo.escape_game_booking.auth.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}