package com.demo.escape_game_booking.booking;

import com.demo.escape_game_booking.common.BusinessRuleException;
import com.demo.escape_game_booking.room.Room;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Booking")
class BookingTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 1, 10, 0);
    private static final LocalDateTime SESSION_START = LocalDateTime.of(2026, 11, 7, 11, 30);

    private final Room room = new Room("L'Asile de Saint-Ombre", "Hôpital psychiatrique",
            "Description", (short) 3, (short) 2, (short) 5, 60, 30, new BigDecimal("30.00"));

    private Booking newBooking() {
        return new Booking(room, "Maud", "maud@example.com", SESSION_START, (short) 4, NOW);
    }

    @Test
    @DisplayName("calcule la fin de session et le prix total à la création")
    void computesEndAndPriceOnCreation() {
        Booking booking = newBooking();

        assertThat(booking.getEndAt()).isEqualTo(LocalDateTime.of(2026, 11, 7, 12, 30));
        assertThat(booking.getTotalPrice()).isEqualByComparingTo("120.00");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("s'annule plus de 24 h avant la session")
    void cancelsMoreThan24HoursBefore() {
        Booking booking = newBooking();

        booking.cancel(SESSION_START.minusDays(2), 24);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("s'annule exactement 24 h avant la session (cas limite)")
    void cancelsExactly24HoursBefore() {
        Booking booking = newBooking();

        booking.cancel(SESSION_START.minusHours(24), 24);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("refuse l'annulation moins de 24 h avant la session")
    void refusesCancellationLessThan24HoursBefore() {
        Booking booking = newBooking();

        assertThatThrownBy(() -> booking.cancel(SESSION_START.minusHours(23), 24))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("24 h");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("refuse d'annuler une réservation déjà annulée")
    void refusesDoubleCancellation() {
        Booking booking = newBooking();
        booking.cancel(SESSION_START.minusDays(2), 24);

        assertThatThrownBy(() -> booking.cancel(SESSION_START.minusDays(2), 24))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("déjà annulée");
    }
}