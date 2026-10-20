package com.demo.escape_game_booking.booking;

import com.demo.escape_game_booking.config.BookingProperties;
import com.demo.escape_game_booking.room.Room;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SlotPlanner")
class SlotPlannerTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 7);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 1, 10, 0);

    private final SlotPlanner slotPlanner = new SlotPlanner(new BookingProperties(10, 22, 60, 24));

    private static Room roomWith(int sessionMinutes, int resetMinutes) {
        return new Room("Salle test", "Thème", "Description", (short) 3, (short) 2, (short) 6,
                sessionMinutes, resetMinutes, new BigDecimal("30.00"));
    }

    @Test
    @DisplayName("génère une grille de 1h30 pour une salle de 60 min + 30 min de reset")
    void generatesNinetyMinuteGrid() {
        List<LocalDateTime> slots = slotPlanner.slotStartsFor(roomWith(60, 30), DAY);

        assertThat(slots).hasSize(8);
        assertThat(slots.getFirst()).isEqualTo(DAY.atTime(10, 0));
        assertThat(slots.get(1)).isEqualTo(DAY.atTime(11, 30));
        assertThat(slots.getLast()).isEqualTo(DAY.atTime(20, 30));
    }

    @Test
    @DisplayName("génère une grille de 1h45 pour une salle de 75 min + 30 min de reset")
    void generatesHundredFiveMinuteGrid() {
        List<LocalDateTime> slots = slotPlanner.slotStartsFor(roomWith(75, 30), DAY);

        assertThat(slots).containsExactly(
                DAY.atTime(10, 0), DAY.atTime(11, 45), DAY.atTime(13, 30), DAY.atTime(15, 15),
                DAY.atTime(17, 0), DAY.atTime(18, 45), DAY.atTime(20, 30));
    }

    @Test
    @DisplayName("valide uniquement les heures présentes dans la grille")
    void validatesOnlyGridSlots() {
        Room room = roomWith(60, 30);

        assertThat(slotPlanner.isValidSlot(room, DAY.atTime(11, 30))).isTrue();
        assertThat(slotPlanner.isValidSlot(room, DAY.atTime(11, 0))).isFalse();
        assertThat(slotPlanner.isValidSlot(room, DAY.atTime(22, 0))).isFalse();
    }

    @Test
    @DisplayName("applique la fenêtre de réservation, bornes comprises")
    void appliesBookingWindow() {
        assertThat(slotPlanner.isInBookingWindow(NOW.minusHours(1), NOW)).isFalse();
        assertThat(slotPlanner.isInBookingWindow(NOW, NOW)).isFalse();
        assertThat(slotPlanner.isInBookingWindow(NOW.plusDays(60), NOW)).isTrue();
        assertThat(slotPlanner.isInBookingWindow(NOW.plusDays(60).plusMinutes(1), NOW)).isFalse();
    }
}