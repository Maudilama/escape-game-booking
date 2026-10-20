package com.demo.escape_game_booking.user;

import com.demo.escape_game_booking.auth.AuthService;
import com.demo.escape_game_booking.config.AdminProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;
    private final Clock clock;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                            AdminProperties adminProperties, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = AuthService.normalizeEmail(adminProperties.email());
        if (userRepository.existsByEmail(email)) {
            return;
        }

        userRepository.save(new User(email, passwordEncoder.encode(adminProperties.password()),
                "Admin", "Escape Game", Role.ADMIN, LocalDateTime.now(clock)));
        log.info("Compte administrateur créé : {}", email);
    }
}