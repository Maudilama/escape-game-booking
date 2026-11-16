package com.demo.escape_game_booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "escape-game.cors")
public record CorsProperties(List<String> allowedOrigins) {
}