package com.demo.escape_game_booking.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "L'email est obligatoire.")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        String password
) {
}