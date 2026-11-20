package com.demo.escape_game_booking.room;

import com.demo.escape_game_booking.common.ErrorCode;
import com.demo.escape_game_booking.common.NotFoundException;

public class RoomNotFoundException extends NotFoundException {

    public RoomNotFoundException(Long id) {
        super(ErrorCode.ROOM_NOT_FOUND, "Salle introuvable : " + id);
    }
}