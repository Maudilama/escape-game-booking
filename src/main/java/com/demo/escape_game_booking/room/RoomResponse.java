package com.demo.escape_game_booking.room;


import java.math.BigDecimal;
import java.util.Locale;

public record RoomResponse(
        Long id,
        String name,
        String theme,
        String description,
        short difficulty,
        short minPlayers,
        short maxPlayers,
        int sessionMinutes,
        BigDecimal pricePerPerson
) {
    public static RoomResponse from(Room room, Locale locale) {
        RoomText text = room.textIn(locale);
        return new RoomResponse(
                room.getId(),
                room.getName(),
                text.theme(),
                text.description(),
                room.getDifficulty(),
                room.getMinPlayers(),
                room.getMaxPlayers(),
                room.getSessionMinutes(),
                room.getPricePerPerson()
        );
    }
}
