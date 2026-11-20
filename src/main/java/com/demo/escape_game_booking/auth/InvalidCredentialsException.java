package com.demo.escape_game_booking.auth;

import com.demo.escape_game_booking.common.ApiException;
import com.demo.escape_game_booking.common.ErrorCode;

public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS, "Email ou mot de passe incorrect.");
    }
}