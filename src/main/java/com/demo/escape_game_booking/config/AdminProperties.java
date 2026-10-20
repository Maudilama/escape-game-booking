package com.demo.escape_game_booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "escape-game.admin")
public record AdminProperties(String email, String password) {
}