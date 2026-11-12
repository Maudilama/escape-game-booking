package com.demo.escape_game_booking.booking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateBookingRequest(

        @NotNull(message = "La salle est obligatoire.")
        Long roomId,

        @NotNull(message = "La date de début est obligatoire.")
        LocalDateTime startAt,

        @NotNull(message = "Le nombre de joueurs est obligatoire.")
        @Min(value = 1, message = "Il faut au moins 1 joueur.")
        @Max(value = 20, message = "Nombre de joueurs trop élevé.")
        Integer playerCount
) {
}