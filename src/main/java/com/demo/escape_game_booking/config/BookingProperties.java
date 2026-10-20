package com.demo.escape_game_booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "escape-game.booking")
public record BookingProperties(
        int openingHour,
        int closingHour,
        int maxDaysInAdvance,
        int cancellationMinHours
) {
}