package com.demo.escape_game_booking.room;

import com.demo.escape_game_booking.common.NotFoundException;

public class RoomNotFoundException extends NotFoundException {

    public RoomNotFoundException(Long id) {
        super("Salle introuvable : " + id);
    }
}