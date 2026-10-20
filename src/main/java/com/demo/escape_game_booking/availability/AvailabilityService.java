package com.demo.escape_game_booking.availability;

import com.demo.escape_game_booking.availability.dto.AvailabilityResponse;
import com.demo.escape_game_booking.availability.dto.SlotResponse;
import com.demo.escape_game_booking.booking.Booking;
import com.demo.escape_game_booking.booking.BookingRepository;
import com.demo.escape_game_booking.booking.BookingStatus;
import com.demo.escape_game_booking.booking.SlotPlanner;
import com.demo.escape_game_booking.room.Room;
import com.demo.escape_game_booking.room.RoomNotFoundException;
import com.demo.escape_game_booking.room.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AvailabilityService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final SlotPlanner slotPlanner;
    private final Clock clock;

    public AvailabilityService(RoomRepository roomRepository,
                               BookingRepository bookingRepository,
                               SlotPlanner slotPlanner,
                               Clock clock) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.slotPlanner = slotPlanner;
        this.clock = clock;
    }

    public AvailabilityResponse getAvailability(Long roomId, LocalDate date) {
        Room room = roomRepository.findByIdAndActiveTrue(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));

        LocalDateTime now = LocalDateTime.now(clock);
        List<Booking> bookingsOfTheDay = bookingRepository
                .findByRoomIdAndStatusAndStartAtLessThanAndEndAtGreaterThan(
                        roomId, BookingStatus.CONFIRMED,
                        date.plusDays(1).atStartOfDay(), date.atStartOfDay());

        List<SlotResponse> slots = slotPlanner.slotStartsFor(room, date).stream()
                .map(start -> toSlot(room, start, now, bookingsOfTheDay))
                .toList();

        return new AvailabilityResponse(roomId, room.getName(), date, slots);
    }

    private SlotResponse toSlot(Room room, LocalDateTime start, LocalDateTime now,
                                List<Booking> bookingsOfTheDay) {
        LocalDateTime end = start.plusMinutes(room.getSessionMinutes());
        boolean booked = bookingsOfTheDay.stream()
                .anyMatch(booking -> booking.overlaps(start, end));
        boolean available = !booked && slotPlanner.isInBookingWindow(start, now);
        return new SlotResponse(start, end, available);
    }
}