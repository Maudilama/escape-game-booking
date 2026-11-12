package com.demo.escape_game_booking.booking;

import com.demo.escape_game_booking.auth.CurrentUser;
import com.demo.escape_game_booking.auth.InvalidCredentialsException;
import com.demo.escape_game_booking.booking.dto.BookingResponse;
import com.demo.escape_game_booking.booking.dto.CreateBookingRequest;
import com.demo.escape_game_booking.common.BusinessRuleException;
import com.demo.escape_game_booking.common.ConflictException;
import com.demo.escape_game_booking.config.BookingProperties;
import com.demo.escape_game_booking.room.Room;
import com.demo.escape_game_booking.room.RoomNotFoundException;
import com.demo.escape_game_booking.room.RoomRepository;
import com.demo.escape_game_booking.user.User;
import com.demo.escape_game_booking.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class BookingService {

    private static final String SLOT_TAKEN = "Ce créneau est déjà réservé.";

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final SlotPlanner slotPlanner;
    private final BookingProperties properties;
    private final Clock clock;

    public BookingService(BookingRepository bookingRepository,
                          RoomRepository roomRepository,
                          UserRepository userRepository,
                          SlotPlanner slotPlanner,
                          BookingProperties properties,
                          Clock clock) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.slotPlanner = slotPlanner;
        this.properties = properties;
        this.clock = clock;
    }

    public BookingResponse create(CreateBookingRequest request, CurrentUser currentUser) {
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

        // Jeton valide mais compte supprimé entre-temps : on refuse
        User user = userRepository.findById(currentUser.id())
                .orElseThrow(InvalidCredentialsException::new);

        Booking booking = new Booking(room, user, startAt, (short) playerCount, now);

        try {
            bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException e) {
            // Deux réservations simultanées : la contrainte PostgreSQL a bloqué la seconde
            throw new ConflictException(SLOT_TAKEN);
        }

        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse findById(Long id, CurrentUser currentUser) {
        return BookingResponse.from(getBookingVisibleTo(id, currentUser));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findMine(CurrentUser currentUser) {
        return bookingRepository.findByUserIdOrderByStartAtDesc(currentUser.id()).stream()
                .map(BookingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findAllOn(LocalDate date) {
        return bookingRepository.findByStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
                        date.atStartOfDay(), date.plusDays(1).atStartOfDay()).stream()
                .map(BookingResponse::from)
                .toList();
    }

    public BookingResponse cancel(Long id, CurrentUser currentUser) {
        Booking booking = getBookingVisibleTo(id, currentUser);
        if (currentUser.admin()) {
            booking.forceCancel();
        } else {
            booking.cancel(LocalDateTime.now(clock), properties.cancellationMinHours());
        }
        return BookingResponse.from(booking);
    }

    /**
     * Un client ne voit que ses réservations ; un admin les voit toutes.
     * Une réservation d'un autre client est traitée comme inexistante (404),
     * pour ne pas révéler son existence.
     */
    private Booking getBookingVisibleTo(Long id, CurrentUser currentUser) {
        return bookingRepository.findById(id)
                .filter(booking -> currentUser.admin() || booking.isOwnedBy(currentUser.id()))
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