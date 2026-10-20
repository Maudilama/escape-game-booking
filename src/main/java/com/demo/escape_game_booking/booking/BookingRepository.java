package com.demo.escape_game_booking.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByRoomIdAndStatusAndStartAtLessThanAndEndAtGreaterThan(
            Long roomId, BookingStatus status, LocalDateTime endAt, LocalDateTime startAt);

    List<Booking> findByRoomIdAndStatusAndStartAtLessThanAndEndAtGreaterThan(
            Long roomId, BookingStatus status, LocalDateTime endAt, LocalDateTime startAt);
}