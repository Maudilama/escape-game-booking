package com.demo.escape_game_booking.config;

import com.demo.escape_game_booking.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CORS (intégration)")
class CorsIntegrationTest extends IntegrationTest {

    private static final String FRONT = "http://localhost:4200";

    @Test
    @DisplayName("autorise la requête de vérification du front pour une réservation")
    void allowsPreflightFromFront() throws Exception {
        mockMvc.perform(options("/api/bookings")
                        .header(HttpHeaders.ORIGIN, FRONT)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT));
    }

    @Test
    @DisplayName("refuse une origine inconnue (403)")
    void rejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/bookings")
                        .header(HttpHeaders.ORIGIN, "https://site-malveillant.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("autorise le front à lire les salles")
    void allowsSimpleRequestFromFront() throws Exception {
        mockMvc.perform(get("/api/rooms").header(HttpHeaders.ORIGIN, FRONT))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT));
    }
}