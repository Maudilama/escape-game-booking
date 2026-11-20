package com.demo.escape_game_booking.booking;

import com.demo.escape_game_booking.common.ErrorCode;
import com.demo.escape_game_booking.common.NotFoundException;

public class BookingNotFoundException extends NotFoundException {

    public BookingNotFoundException(Long id) {
        super(ErrorCode.BOOKING_NOT_FOUND, "Réservation introuvable : " + id);
    }
}