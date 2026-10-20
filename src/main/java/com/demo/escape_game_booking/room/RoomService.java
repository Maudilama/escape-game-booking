package com.demo.escape_game_booking.room;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<RoomResponse> findAllActive() {
        return roomRepository.findAllByActiveTrueOrderByIdAsc()
                .stream()
                .map(RoomResponse::from)
                .toList();
    }

    public RoomResponse findById(Long id) {
        return roomRepository.findByIdAndActiveTrue(id)
                .map(RoomResponse::from)
                .orElseThrow(() -> new RoomNotFoundException(id));
    }
}