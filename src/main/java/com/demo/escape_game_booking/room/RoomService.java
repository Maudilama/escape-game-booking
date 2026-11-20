package com.demo.escape_game_booking.room;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<RoomResponse> findAllActive(Locale locale) {
        return roomRepository.findAllWithTranslationsByActiveTrueOrderByIdAsc()
                .stream()
                .map(room -> RoomResponse.from(room, locale))
                .toList();
    }

    public RoomResponse findById(Long id, Locale locale) {
        return roomRepository.findWithTranslationsByIdAndActiveTrue(id)
                .map(room -> RoomResponse.from(room, locale))
                .orElseThrow(() -> new RoomNotFoundException(id));
    }
}