package com.demo.escape_game_booking.booking;

import com.demo.escape_game_booking.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Réservations (intégration)")
class BookingIntegrationTest extends IntegrationTest {

    @Nested
    @DisplayName("Parcours de réservation")
    class BookingFlow {

        @Test
        @DisplayName("une réservation est créée et le créneau devient indisponible")
        void createsBookingAndUpdatesAvailability() throws Exception {
            String alice = registerAndLogin("alice@example.com");
            LocalDateTime slot = nextSaturdayAt(11, 30);

            createBooking(alice, slot)
                    .andExpect(status().isCreated())
                    .andExpect(header().exists(HttpHeaders.LOCATION))
                    .andExpect(jsonPath("$.customerEmail").value("alice@example.com"))
                    .andExpect(jsonPath("$.totalPrice").value(120.0))
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));

            // Grille de l'Asile : 10:00 (index 0), 11:30 (index 1)…
            mockMvc.perform(get("/api/rooms/{id}/availability", ASILE_ID)
                            .param("date", slot.toLocalDate().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slots[0].available").value(true))
                    .andExpect(jsonPath("$.slots[1].available").value(false));
        }

        @Test
        @DisplayName("un autre client ne peut ni voir ni annuler la réservation (404)")
        void otherCustomerGetsNotFound() throws Exception {
            String alice = registerAndLogin("alice@example.com");
            String bob = registerAndLogin("bob@example.com");
            long bookingId = idOf(createBooking(alice, nextSaturdayAt(11, 30)));

            mockMvc.perform(get("/api/bookings/{id}", bookingId)
                            .header(HttpHeaders.AUTHORIZATION, bearer(bob)))
                    .andExpect(status().isNotFound());

            mockMvc.perform(patch("/api/bookings/{id}/cancel", bookingId)
                            .header(HttpHeaders.AUTHORIZATION, bearer(bob)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("l'admin voit le planning du jour avec les réservations de tous les clients")
        void adminSeesDailyPlanning() throws Exception {
            String alice = registerAndLogin("alice@example.com");
            LocalDateTime slot = nextSaturdayAt(11, 30);
            createBooking(alice, slot).andExpect(status().isCreated());

            mockMvc.perform(get("/api/admin/bookings")
                            .param("date", slot.toLocalDate().toString())
                            .header(HttpHeaders.AUTHORIZATION, bearer(loginAsAdmin())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].customerEmail").value("alice@example.com"));
        }

        @Test
        @DisplayName("un créneau annulé peut être réservé à nouveau")
        void cancelledSlotCanBeBookedAgain() throws Exception {
            String alice = registerAndLogin("alice@example.com");
            String bob = registerAndLogin("bob@example.com");
            LocalDateTime slot = nextSaturdayAt(11, 30);
            long bookingId = idOf(createBooking(alice, slot));

            mockMvc.perform(patch("/api/bookings/{id}/cancel", bookingId)
                            .header(HttpHeaders.AUTHORIZATION, bearer(alice)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));

            createBooking(bob, slot).andExpect(status().isCreated());
        }

        @Test
        @DisplayName("refuse une réservation un lundi (422)")
        void rejectsBookingOnClosedDay() throws Exception {
            String alice = registerAndLogin("alice@example.com");

            createBooking(alice, upcomingAt(DayOfWeek.MONDAY, 11, 30))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.detail").value("L'escape game est fermé ce jour-là."));
        }
    }

    @Nested
    @DisplayName("Protection contre les doubles réservations")
    class DoubleBookingProtection {

        @Test
        @DisplayName("de deux réservations simultanées du même créneau, une seule réussit")
        void onlyOneOfTwoSimultaneousBookingsSucceeds() throws Exception {
            String alice = registerAndLogin("alice@example.com");
            String bob = registerAndLogin("bob@example.com");
            String body = bookingJson(ASILE_ID, nextSaturdayAt(11, 30), 4);
            CountDownLatch startSignal = new CountDownLatch(1);

            try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
                Future<Integer> aliceStatus = executor.submit(() -> {
                    startSignal.await();
                    return postBooking(alice, body);
                });
                Future<Integer> bobStatus = executor.submit(() -> {
                    startSignal.await();
                    return postBooking(bob, body);
                });

                startSignal.countDown(); // les deux requêtes partent en même temps

                assertThat(List.of(aliceStatus.get(), bobStatus.get()))
                        .containsExactlyInAnyOrder(201, 409);
            }
        }

        @Test
        @DisplayName("PostgreSQL refuse deux réservations confirmées qui se chevauchent")
        void databaseRejectsOverlappingConfirmedBookings() {
            Long adminId = adminId();
            LocalDateTime start = nextSaturdayAt(11, 30);
            insertBooking(adminId, start, "CONFIRMED");

            assertThatThrownBy(() -> insertBooking(adminId, start.plusMinutes(30), "CONFIRMED"))
                    .isInstanceOf(DataAccessException.class)
                    .hasMessageContaining("bookings_no_overlap");
        }

        @Test
        @DisplayName("PostgreSQL ignore les réservations annulées dans la contrainte")
        void databaseIgnoresCancelledBookings() {
            Long adminId = adminId();
            LocalDateTime start = nextSaturdayAt(11, 30);
            insertBooking(adminId, start, "CANCELLED");

            assertThatCode(() -> insertBooking(adminId, start, "CONFIRMED"))
                    .doesNotThrowAnyException();
        }

        private int postBooking(String token, String body) throws Exception {
            ResultActions result = mockMvc.perform(post("/api/bookings")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body));
            return result.andReturn().getResponse().getStatus();
        }

        private Long adminId() {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM users WHERE role = 'ADMIN'", Long.class);
        }

        /** Insertion SQL directe : on contourne volontairement le code Java pour tester la base seule. */
        private void insertBooking(Long userId, LocalDateTime start, String status) {
            jdbcTemplate.update("""
                            INSERT INTO bookings (room_id, user_id, start_at, end_at, player_count,
                                                  total_price, status, created_at)
                            VALUES (?, ?, ?, ?, 4, 120.00, ?, now())
                            """,
                    ASILE_ID, userId, Timestamp.valueOf(start),
                    Timestamp.valueOf(start.plusHours(1)), status);
        }
    }
}