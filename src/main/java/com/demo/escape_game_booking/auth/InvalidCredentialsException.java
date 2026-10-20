package com.demo.escape_game_booking.auth;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Email ou mot de passe incorrect.");
    }
}