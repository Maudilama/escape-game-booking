package com.demo.escape_game_booking.booking;

import com.demo.escape_game_booking.config.BookingProperties;
import com.demo.escape_game_booking.room.Room;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Source unique des règles de planning : grille des créneaux d'une salle
 * et fenêtre de réservation.
 */
@Component
public class SlotPlanner {

    private final BookingProperties properties;

    public SlotPlanner(BookingProperties properties) {
        this.properties = properties;
    }

    public List<LocalDateTime> slotStartsFor(Room room, LocalDate date) {
        if (isClosed(date)) {
            return List.of();
        }
        LocalDateTime opening = date.atTime(properties.openingHour(), 0);
        LocalDateTime closing = date.atTime(properties.closingHour(), 0);
        int step = room.getSessionMinutes() + room.getResetMinutes();

        List<LocalDateTime> starts = new ArrayList<>();
        for (LocalDateTime start = opening;
             !start.plusMinutes(room.getSessionMinutes()).isAfter(closing);
             start = start.plusMinutes(step)) {
            starts.add(start);
        }
        return starts;
    }

    public boolean isClosed(LocalDate date) {
        return properties.closedDays().contains(date.getDayOfWeek());
    }

    public boolean isValidSlot(Room room, LocalDateTime startAt) {
        return slotStartsFor(room, startAt.toLocalDate()).contains(startAt);
    }

    public boolean isInBookingWindow(LocalDateTime startAt, LocalDateTime now) {
        return startAt.isAfter(now)
                && !startAt.isAfter(now.plusDays(properties.maxDaysInAdvance()));
    }
}