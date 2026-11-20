package com.demo.escape_game_booking.common;

public abstract class NotFoundException extends ApiException {

    protected NotFoundException(ErrorCode code, String message) {
        super(code, message);
    }
}