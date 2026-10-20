package com.demo.escape_game_booking.booking;

import com.demo.escape_game_booking.booking.dto.BookingResponse;
import com.demo.escape_game_booking.booking.dto.CreateBookingRequest;
import com.demo.escape_game_booking.common.BusinessRuleException;
import com.demo.escape_game_booking.common.ConflictException;
import com.demo.escape_game_booking.config.BookingProperties;
import com.demo.escape_game_booking.room.Room;
import com.demo.escape_game_booking.room.RoomNotFoundException;
import com.demo.escape_game_booking.room.RoomRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@Transactional
public class BookingService {

    private static final String SLOT_TAKEN = "Ce créneau est déjà réservé.";

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final SlotPlanner slotPlanner;
    private final BookingProperties properties;
    private final Clock clock;

    public BookingService(BookingRepository bookingRepository,
                          RoomRepository roomRepository,
                          SlotPlanner slotPlanner,
                          BookingProperties properties,
                          Clock clock) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.slotPlanner = slotPlanner;
        this.properties = properties;
        this.clock = clock;
    }

    public BookingResponse create(CreateBookingRequest request) {
        Room room = roomRepository.findByIdAndActiveTrue(request.roomId())
                .orElseThrow(() -> new RoomNotFoundException(request.roomId()));

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime startAt = request.startAt();
        int playerCount = request.playerCount();

        checkPlayerCount(room, playerCount);
        checkBookingWindow(startAt, now);
        checkSlot(room, startAt);

        LocalDateTime endAt = startAt.plusMinutes(room.getSessionMinutes());
        boolean slotTaken = bookingRepository
                .existsByRoomIdAndStatusAndStartAtLessThanAndEndAtGreaterThan(
                        room.getId(), BookingStatus.CONFIRMED, endAt, startAt);
        if (slotTaken) {
            throw new ConflictException(SLOT_TAKEN);
        }

        Booking booking = new Booking(room, request.customerName(), request.customerEmail(),
                startAt, (short) playerCount, now);

        try {
            bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException e) {
            // Deux réservations simultanées : la contrainte PostgreSQL a bloqué la seconde
            throw new ConflictException(SLOT_TAKEN);
        }

        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse findById(Long id) {
        return BookingResponse.from(getBooking(id));
    }

    public BookingResponse cancel(Long id) {
        Booking booking = getBooking(id);
        booking.cancel(LocalDateTime.now(clock), properties.cancellationMinHours());
        return BookingResponse.from(booking);
    }

    private Booking getBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));
    }

    private void checkPlayerCount(Room room, int playerCount) {
        if (!room.acceptsPlayerCount(playerCount)) {
            throw new BusinessRuleException(
                    "La salle « " + room.getName() + " » accepte de "
                            + room.getMinPlayers() + " à " + room.getMaxPlayers() + " joueurs.");
        }
    }

    private void checkBookingWindow(LocalDateTime startAt, LocalDateTime now) {
        if (slotPlanner.isInBookingWindow(startAt, now)) {
            return;
        }
        if (!startAt.isAfter(now)) {
            throw new BusinessRuleException("Impossible de réserver un créneau passé.");
        }
        throw new BusinessRuleException(
                "Les réservations sont ouvertes " + properties.maxDaysInAdvance()
                        + " jours à l'avance maximum.");
    }

    private void checkSlot(Room room, LocalDateTime startAt) {
        if (!slotPlanner.isValidSlot(room, startAt)) {
            throw new BusinessRuleException(
                    "L'heure de début ne correspond à aucun créneau de cette salle.");
        }
    }
}