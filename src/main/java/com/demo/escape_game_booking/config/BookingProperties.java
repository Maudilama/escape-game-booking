package com.demo.escape_game_booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.DayOfWeek;
import java.util.List;

@ConfigurationProperties(prefix = "escape-game.booking")
public record BookingProperties(
        int openingHour,
        int closingHour,
        int maxDaysInAdvance,
        int cancellationMinHours,
        List<DayOfWeek> closedDays
) {
    public BookingProperties {
        closedDays = closedDays == null ? List.of() : List.copyOf(closedDays);
    }
}