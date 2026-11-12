package com.demo.escape_game_booking.availability;

import com.demo.escape_game_booking.availability.dto.AvailabilityResponse;
import com.demo.escape_game_booking.availability.dto.SlotResponse;
import com.demo.escape_game_booking.booking.Booking;
import com.demo.escape_game_booking.booking.BookingRepository;
import com.demo.escape_game_booking.booking.SlotPlanner;
import com.demo.escape_game_booking.config.BookingProperties;
import com.demo.escape_game_booking.room.Room;
import com.demo.escape_game_booking.room.RoomNotFoundException;
import com.demo.escape_game_booking.room.RoomRepository;
import com.demo.escape_game_booking.user.Role;
import com.demo.escape_game_booking.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AvailabilityService")
class AvailabilityServiceTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 1, 10, 0);
    private static final LocalDate DAY = LocalDate.of(2026, 11, 7);
    private static final Long ROOM_ID = 1L;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BookingRepository bookingRepository;

    private AvailabilityService availabilityService;
    private Room room;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW.atZone(PARIS).toInstant(), PARIS);
        SlotPlanner slotPlanner = new SlotPlanner(new BookingProperties(10, 22, 60, 24));
        availabilityService = new AvailabilityService(roomRepository, bookingRepository,
                slotPlanner, fixedClock);

        room = new Room("L'Asile de Saint-Ombre", "Hôpital psychiatrique", "Description",
                (short) 3, (short) 2, (short) 5, 60, 30, new BigDecimal("30.00"));
    }

    private SlotResponse slotAt(AvailabilityResponse response, LocalDateTime start) {
        return response.slots().stream()
                .filter(slot -> slot.startAt().equals(start))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("marque comme indisponible un créneau déjà réservé")
    void marksBookedSlotUnavailable() {
        when(roomRepository.findByIdAndActiveTrue(ROOM_ID)).thenReturn(Optional.of(room));
        User customer = new User("maud@example.com", "hash", "Maud", "Torchy", Role.CUSTOMER, NOW);
        Booking booking = new Booking(room, customer, DAY.atTime(11, 30), (short) 4, NOW);
        when(bookingRepository.findByRoomIdAndStatusAndStartAtLessThanAndEndAtGreaterThan(
                any(), any(), any(), any())).thenReturn(List.of(booking));

        AvailabilityResponse response = availabilityService.getAvailability(ROOM_ID, DAY);

        assertThat(response.slots()).hasSize(8);
        assertThat(slotAt(response, DAY.atTime(11, 30)).available()).isFalse();
        assertThat(slotAt(response, DAY.atTime(10, 0)).available()).isTrue();
    }

    @Test
    @DisplayName("marque comme indisponibles les créneaux déjà passés")
    void marksPastSlotsUnavailable() {
        when(roomRepository.findByIdAndActiveTrue(ROOM_ID)).thenReturn(Optional.of(room));
        LocalDate today = NOW.toLocalDate();

        AvailabilityResponse response = availabilityService.getAvailability(ROOM_ID, today);

        assertThat(slotAt(response, today.atTime(10, 0)).available()).isFalse();
        assertThat(slotAt(response, today.atTime(11, 30)).available()).isTrue();
    }

    @Test
    @DisplayName("refuse une salle inexistante")
    void rejectsUnknownRoom() {
        when(roomRepository.findByIdAndActiveTrue(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> availabilityService.getAvailability(ROOM_ID, DAY))
                .isInstanceOf(RoomNotFoundException.class);
    }
}