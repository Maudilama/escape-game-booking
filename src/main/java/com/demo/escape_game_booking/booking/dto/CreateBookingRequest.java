package com.demo.escape_game_booking.booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateBookingRequest(

        @NotNull(message = "La salle est obligatoire.")
        Long roomId,

        @NotNull(message = "La date de début est obligatoire.")
        LocalDateTime startAt,

        @NotNull(message = "Le nombre de joueurs est obligatoire.")
        @Min(value = 1, message = "Il faut au moins 1 joueur.")
        @Max(value = 20, message = "Nombre de joueurs trop élevé.")
        Integer playerCount,

        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères.")
        String customerName,

        @NotBlank(message = "L'email est obligatoire.")
        @Email(message = "L'email n'est pas valide.")
        @Size(max = 255, message = "L'email ne doit pas dépasser 255 caractères.")
        String customerEmail
) {
}