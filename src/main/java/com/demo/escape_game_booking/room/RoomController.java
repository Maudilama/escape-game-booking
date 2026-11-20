package com.demo.escape_game_booking.room;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getRooms(Locale locale) {
        return localized(locale).body(roomService.findAllActive(locale));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable Long id, Locale locale) {
        return localized(locale).body(roomService.findById(id, locale));
    }

    private static ResponseEntity.BodyBuilder localized(Locale locale) {
        return ResponseEntity.ok()
                .varyBy(HttpHeaders.ACCEPT_LANGUAGE)
                .header(HttpHeaders.CONTENT_LANGUAGE, locale.getLanguage());
    }
}