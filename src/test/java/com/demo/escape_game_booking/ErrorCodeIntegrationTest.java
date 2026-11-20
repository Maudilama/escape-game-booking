package com.demo.escape_game_booking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.time.DayOfWeek;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Codes d'erreur des ProblemDetail")
class ErrorCodeIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("une règle métier renvoie son code et ses paramètres")
    void businessRuleCarriesCodeAndParams() throws Exception {
        String token = registerAndLogin("codes@example.com");

        mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(ASILE_ID, upcomingAt(DayOfWeek.SATURDAY, 10, 0), 9)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("PLAYER_COUNT_OUT_OF_RANGE"))
                .andExpect(jsonPath("$.params.min").value(2))
                .andExpect(jsonPath("$.params.max").value(5));
    }

    @Test
    @DisplayName("une ressource introuvable renvoie son code")
    void notFoundCarriesCode() throws Exception {
        mockMvc.perform(get("/api/rooms/{id}", 999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    @DisplayName("des identifiants incorrects renvoient un code, sans préciser lequel est faux")
    void invalidCredentialsCarriesCode() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "personne@example.com", "password": "mauvais"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }
}