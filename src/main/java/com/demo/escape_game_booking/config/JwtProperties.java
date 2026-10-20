package com.demo.escape_game_booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "escape-game.jwt")
public record JwtProperties(String secret, Duration tokenValidity) {
}