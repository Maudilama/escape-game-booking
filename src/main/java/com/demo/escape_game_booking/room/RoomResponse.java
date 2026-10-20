package com.demo.escape_game_booking.room;


import java.math.BigDecimal;

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
    public static RoomResponse from(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getTheme(),
                room.getDescription(),
                room.getDifficulty(),
                room.getMinPlayers(),
                room.getMaxPlayers(),
                room.getSessionMinutes(),
                room.getPricePerPerson()
        );
    }
}
