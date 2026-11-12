package com.demo.escape_game_booking.booking;

import com.demo.escape_game_booking.auth.CurrentUser;
import com.demo.escape_game_booking.booking.dto.BookingResponse;
import com.demo.escape_game_booking.booking.dto.CreateBookingRequest;
import com.demo.escape_game_booking.common.BusinessRuleException;
import com.demo.escape_game_booking.common.ConflictException;
import com.demo.escape_game_booking.config.BookingProperties;
import com.demo.escape_game_booking.room.Room;
import com.demo.escape_game_booking.room.RoomNotFoundException;
import com.demo.escape_game_booking.room.RoomRepository;
import com.demo.escape_game_booking.user.Role;
import com.demo.escape_game_booking.user.User;
import com.demo.escape_game_booking.user.UserRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
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
    private static final Long BOOKING_ID = 10L;

    private static final CurrentUser CUSTOMER = new CurrentUser(1L, false);
    private static final CurrentUser OTHER_CUSTOMER = new CurrentUser(2L, false);
    private static final CurrentUser ADMIN = new CurrentUser(3L, true);

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    private BookingService bookingService;
    private Room room;
    private User customer;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW.atZone(PARIS).toInstant(), PARIS);
        BookingProperties properties = new BookingProperties(10, 22, 60, 24);
        SlotPlanner slotPlanner = new SlotPlanner(properties);
        bookingService = new BookingService(bookingRepository, roomRepository, userRepository,
                slotPlanner, properties, fixedClock);

        room = new Room("L'Asile de Saint-Ombre", "Hôpital psychiatrique", "Description",
                (short) 3, (short) 2, (short) 5, 60, 30, new BigDecimal("30.00"));

        customer = new User("maud@example.com", "hash", "Maud", "Torchy", Role.CUSTOMER, NOW);
        ReflectionTestUtils.setField(customer, "id", CUSTOMER.id());
    }

    private CreateBookingRequest request(LocalDateTime startAt, int players) {
        return new CreateBookingRequest(ROOM_ID, startAt, players);
    }

    private void givenRoomExists() {
        when(roomRepository.findByIdAndActiveTrue(ROOM_ID)).thenReturn(Optional.of(room));
    }

    private void givenCustomerExists() {
        when(userRepository.findById(CUSTOMER.id())).thenReturn(Optional.of(customer));
    }

    private Booking givenCustomerBookingAt(LocalDateTime startAt) {
        Booking booking = new Booking(room, customer, startAt, (short) 4, NOW);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        return booking;
    }

    private void assertRejected(CreateBookingRequest request, String expectedMessagePart) {
        assertThatThrownBy(() -> bookingService.create(request, CUSTOMER))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(expectedMessagePart);

        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Nested
    @DisplayName("Création d'une réservation")
    class Create {

        @Test
        @DisplayName("enregistre une réservation valide au nom de l'utilisateur connecté")
        void savesValidBooking() {
            givenRoomExists();
            givenCustomerExists();

            BookingResponse response = bookingService.create(request(VALID_SLOT, 4), CUSTOMER);

            ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
            verify(bookingRepository).saveAndFlush(captor.capture());
            Booking saved = captor.getValue();

            assertThat(saved.getStartAt()).isEqualTo(VALID_SLOT);
            assertThat(saved.isOwnedBy(CUSTOMER.id())).isTrue();
            assertThat(response.customerEmail()).isEqualTo("maud@example.com");
            assertThat(response.totalPrice()).isEqualByComparingTo("120.00");
        }

        @Test
        @DisplayName("refuse une salle inexistante")
        void rejectsUnknownRoom() {
            when(roomRepository.findByIdAndActiveTrue(ROOM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.create(request(VALID_SLOT, 4), CUSTOMER))
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

            assertRejected(request(LocalDateTime.of(2026, 11, 7, 22, 0), 4), "aucun créneau");
        }

        @Test
        @DisplayName("refuse un créneau déjà réservé")
        void rejectsAlreadyBookedSlot() {
            givenRoomExists();
            when(bookingRepository.existsByRoomIdAndStatusAndStartAtLessThanAndEndAtGreaterThan(
                    any(), any(), any(), any())).thenReturn(true);

            assertThatThrownBy(() -> bookingService.create(request(VALID_SLOT, 4), CUSTOMER))
                    .isInstanceOf(ConflictException.class);

            verify(bookingRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("convertit une réservation simultanée bloquée par la base en conflit")
        void convertsConcurrentBookingIntoConflict() {
            givenRoomExists();
            givenCustomerExists();
            when(bookingRepository.saveAndFlush(any()))
                    .thenThrow(new DataIntegrityViolationException("bookings_no_overlap"));

            assertThatThrownBy(() -> bookingService.create(request(VALID_SLOT, 4), CUSTOMER))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("déjà réservé");
        }
    }

    @Nested
    @DisplayName("Droits d'accès aux réservations")
    class Access {

        @Test
        @DisplayName("un client voit sa propre réservation")
        void ownerCanSeeBooking() {
            givenCustomerBookingAt(VALID_SLOT);

            BookingResponse response = bookingService.findById(BOOKING_ID, CUSTOMER);

            assertThat(response.customerEmail()).isEqualTo("maud@example.com");
        }

        @Test
        @DisplayName("un client ne voit pas la réservation d'un autre (404)")
        void otherCustomerCannotSeeBooking() {
            givenCustomerBookingAt(VALID_SLOT);

            assertThatThrownBy(() -> bookingService.findById(BOOKING_ID, OTHER_CUSTOMER))
                    .isInstanceOf(BookingNotFoundException.class);
        }

        @Test
        @DisplayName("un admin voit toutes les réservations")
        void adminCanSeeAnyBooking() {
            givenCustomerBookingAt(VALID_SLOT);

            BookingResponse response = bookingService.findById(BOOKING_ID, ADMIN);

            assertThat(response.customerEmail()).isEqualTo("maud@example.com");
        }

        @Test
        @DisplayName("liste les réservations de l'utilisateur connecté")
        void listsMyBookings() {
            Booking booking = new Booking(room, customer, VALID_SLOT, (short) 4, NOW);
            when(bookingRepository.findByUserIdOrderByStartAtDesc(CUSTOMER.id()))
                    .thenReturn(List.of(booking));

            List<BookingResponse> bookings = bookingService.findMine(CUSTOMER);

            assertThat(bookings).hasSize(1);
        }

        @Test
        @DisplayName("un client ne peut pas annuler la réservation d'un autre")
        void otherCustomerCannotCancel() {
            Booking booking = givenCustomerBookingAt(VALID_SLOT);

            assertThatThrownBy(() -> bookingService.cancel(BOOKING_ID, OTHER_CUSTOMER))
                    .isInstanceOf(BookingNotFoundException.class);

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("un client ne peut pas annuler moins de 24 h avant")
        void customerCannotCancelLate() {
            givenCustomerBookingAt(NOW.plusHours(3));

            assertThatThrownBy(() -> bookingService.cancel(BOOKING_ID, CUSTOMER))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        @DisplayName("un admin peut annuler à tout moment")
        void adminCanCancelLate() {
            givenCustomerBookingAt(NOW.plusHours(3));

            BookingResponse response = bookingService.cancel(BOOKING_ID, ADMIN);

            assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        }

        @Test
        @DisplayName("refuse une réservation inexistante")
        void rejectsUnknownBooking() {
            when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.cancel(99L, CUSTOMER))
                    .isInstanceOf(BookingNotFoundException.class);
        }
    }
}