package com.demo.escape_game_booking.availability.dto;

import java.time.LocalDate;
import java.util.List;

public record AvailabilityResponse(
        Long roomId,
        String roomName,
        LocalDate date,
        List<SlotResponse> slots
) {
}