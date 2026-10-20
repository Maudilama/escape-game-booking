package com.demo.escape_game_booking.booking;

import com.demo.escape_game_booking.booking.dto.BookingResponse;
import com.demo.escape_game_booking.booking.dto.CreateBookingRequest;
import com.demo.escape_game_booking.common.BusinessRuleException;
import com.demo.escape_game_booking.common.ConflictException;
import com.demo.escape_game_booking.config.BookingProperties;
import com.demo.escape_game_booking.room.Room;
import com.demo.escape_game_booking.room.RoomNotFoundException;
import com.demo.escape_game_booking.room.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService")
class BookingServiceTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    // « Maintenant », pour tous les tests : le 1er novembre 2026 à 10h
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 1, 10, 0);
    // Créneau valide pour l'Asile (grille de 1h30 : 10:00, 11:30, 13:00…)
    private static final LocalDateTime VALID_SLOT = LocalDateTime.of(2026, 11, 7, 11, 30);
    private static final Long ROOM_ID = 1L;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    private BookingService bookingService;
    private Room room;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW.atZone(PARIS).toInstant(), PARIS);
        BookingProperties properties = new BookingProperties(10, 22, 60, 24);
        bookingService = new BookingService(bookingRepository, roomRepository, properties, fixedClock);

        room = new Room("L'Asile de Saint-Ombre", "Hôpital psychiatrique", "Description",
                (short) 3, (short) 2, (short) 5, 60, 30, new BigDecimal("30.00"));
    }

    private CreateBookingRequest request(LocalDateTime startAt, int players) {
        return new CreateBookingRequest(ROOM_ID, startAt, players, "Maud", "maud@example.com");
    }

    private void givenRoomExists() {
        when(roomRepository.findByIdAndActiveTrue(ROOM_ID)).thenReturn(Optional.of(room));
    }

    private void assertRejected(CreateBookingRequest request, String expectedMessagePart) {
        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(expectedMessagePart);

        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Nested
    @DisplayName("Création d'une réservation")
    class Create {

        @Test
        @DisplayName("enregistre une réservation valide")
        void savesValidBooking() {
            givenRoomExists();

            BookingResponse response = bookingService.create(request(VALID_SLOT, 4));

            ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
            verify(bookingRepository).saveAndFlush(captor.capture());
            Booking saved = captor.getValue();

            assertThat(saved.getStartAt()).isEqualTo(VALID_SLOT);
            assertThat(saved.getCreatedAt()).isEqualTo(NOW);
            assertThat(response.totalPrice()).isEqualByComparingTo("120.00");
            assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("refuse une salle inexistante")
        void rejectsUnknownRoom() {
            when(roomRepository.findByIdAndActiveTrue(ROOM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.create(request(VALID_SLOT, 4)))
                    .isInstanceOf(RoomNotFoundException.class);

            verify(bookingRepository, never()).saveAndFlush(any());
        }

        @ParameterizedTest(name = "refuse {0} joueur(s) pour une salle de 2 à 5")
        @ValueSource(ints = {1, 6})
        void rejectsInvalidPlayerCount(int players) {
            givenRoomExists();

            assertRejected(request(VALID_SLOT, players), "joueurs");
        }

        @Test
        @DisplayName("refuse un créneau passé")
        void rejectsPastSlot() {
            givenRoomExists();

            assertRejected(request(LocalDateTime.of(2026, 10, 31, 10, 0), 4), "passé");
        }

        @Test
        @DisplayName("refuse un créneau à plus de 60 jours")
        void rejectsSlotTooFarInAdvance() {
            givenRoomExists();

            assertRejected(request(LocalDateTime.of(2027, 1, 1, 10, 0), 4), "60 jours");
        }

        @Test
        @DisplayName("refuse une heure qui ne tombe pas sur la grille de la salle")
        void rejectsSlotOffGrid() {
            givenRoomExists();

            assertRejected(request(LocalDateTime.of(2026, 11, 7, 11, 0), 4), "aucun créneau");
        }

        @Test
        @DisplayName("refuse une session qui finirait après la fermeture")
        void rejectsSlotAfterClosing() {
            givenRoomExists();

            assertRejected(request(LocalDateTime.of(2026, 11, 7, 22, 0), 4), "horaires d'ouverture");
        }

        @Test
        @DisplayName("refuse un créneau déjà réservé")
        void rejectsAlreadyBookedSlot() {
            givenRoomExists();
            when(bookingRepository.existsByRoomIdAndStatusAndStartAtLessThanAndEndAtGreaterThan(
                    any(), any(), any(), any())).thenReturn(true);

            assertThatThrownBy(() -> bookingService.create(request(VALID_SLOT, 4)))
                    .isInstanceOf(ConflictException.class);

            verify(bookingRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("convertit une réservation simultanée bloquée par la base en conflit")
        void convertsConcurrentBookingIntoConflict() {
            givenRoomExists();
            when(bookingRepository.saveAndFlush(any()))
                    .thenThrow(new DataIntegrityViolationException("bookings_no_overlap"));

            assertThatThrownBy(() -> bookingService.create(request(VALID_SLOT, 4)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("déjà réservé");
        }
    }

    @Nested
    @DisplayName("Annulation d'une réservation")
    class Cancel {

        @Test
        @DisplayName("annule une réservation existante")
        void cancelsExistingBooking() {
            Booking booking = new Booking(room, "Maud", "maud@example.com", VALID_SLOT, (short) 4, NOW);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            BookingResponse response = bookingService.cancel(1L);

            assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        }

        @Test
        @DisplayName("refuse une réservation inexistante")
        void rejectsUnknownBooking() {
            when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.cancel(99L))
                    .isInstanceOf(BookingNotFoundException.class);
        }
    }
}