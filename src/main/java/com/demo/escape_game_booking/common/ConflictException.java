package com.demo.escape_game_booking.common;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}