package com.demo.escape_game_booking.room;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Le texte d'une salle dans une langue. Un record : Hibernate sait l'utiliser comme objet intégré. */
@Embeddable
public record RoomText(
        @Column(name = "theme", nullable = false, length = 100) String theme,
        @Column(name = "description", nullable = false, columnDefinition = "TEXT") String description
) {
}