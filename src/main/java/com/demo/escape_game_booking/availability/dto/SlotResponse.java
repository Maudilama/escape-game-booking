package com.demo.escape_game_booking.availability.dto;

import java.time.LocalDateTime;

public record SlotResponse(
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean available
) {
}