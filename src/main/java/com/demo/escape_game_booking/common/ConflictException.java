package com.demo.escape_game_booking.common;

public class ConflictException extends ApiException {

    public ConflictException(ErrorCode code, String message) {
        super(code, message);
    }
}