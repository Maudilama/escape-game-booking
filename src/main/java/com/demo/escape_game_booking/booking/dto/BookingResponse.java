package com.demo.escape_game_booking.booking.dto;

import com.demo.escape_game_booking.booking.Booking;
import com.demo.escape_game_booking.booking.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long roomId,
        String roomName,
        String customerName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        short playerCount,
        BigDecimal totalPrice,
        BookingStatus status
) {
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getRoom().getId(),
                booking.getRoom().getName(),
                booking.getCustomerName(),
                booking.getStartAt(),
                booking.getEndAt(),
                booking.getPlayerCount(),
                booking.getTotalPrice(),
                booking.getStatus()
        );
    }
}