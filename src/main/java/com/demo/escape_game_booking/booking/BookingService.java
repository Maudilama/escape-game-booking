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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@Transactional
public class BookingService {

    private static final String SLOT_TAKEN = "Ce créneau est déjà réservé.";

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final BookingProperties properties;
    private final Clock clock;

    public BookingService(BookingRepository bookingRepository,
                          RoomRepository roomRepository,
                          BookingProperties properties,
                          Clock clock) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
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
        checkSlotAlignment(room, startAt);

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
        if (!startAt.isAfter(now)) {
            throw new BusinessRuleException("Impossible de réserver un créneau passé.");
        }
        if (startAt.isAfter(now.plusDays(properties.maxDaysInAdvance()))) {
            throw new BusinessRuleException(
                    "Les réservations sont ouvertes " + properties.maxDaysInAdvance()
                            + " jours à l'avance maximum.");
        }
    }

    private void checkSlotAlignment(Room room, LocalDateTime startAt) {
        LocalTime opening = LocalTime.of(properties.openingHour(), 0);
        LocalTime closing = LocalTime.of(properties.closingHour(), 0);
        LocalTime start = startAt.toLocalTime();
        LocalTime end = start.plusMinutes(room.getSessionMinutes());

        boolean outsideOpeningHours = start.isBefore(opening)
                || end.isAfter(closing)
                || end.isBefore(start); // la session déborderait après minuit
        if (outsideOpeningHours) {
            throw new BusinessRuleException("Ce créneau est en dehors des horaires d'ouverture.");
        }

        long minutesSinceOpening = Duration.between(opening, start).toMinutes();
        int slotLength = room.getSessionMinutes() + room.getResetMinutes();
        boolean onGrid = minutesSinceOpening % slotLength == 0
                && start.getSecond() == 0
                && start.getNano() == 0;
        if (!onGrid) {
            throw new BusinessRuleException(
                    "L'heure de début ne correspond à aucun créneau de cette salle.");
        }
    }
}