package com.demo.escape_game_booking.auth;

import com.demo.escape_game_booking.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Authentification et sécurité (intégration)")
class AuthIntegrationTest extends IntegrationTest {

    private static final String REGISTER_ALICE = """
            {"email": "alice@example.com", "password": "motdepasse123",
             "firstName": "Alice", "lastName": "Martin"}
            """;

    @Test
    @DisplayName("l'inscription crée un client sans jamais exposer le mot de passe")
    void registersCustomer() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_ALICE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("refuse un email déjà utilisé, sans tenir compte des majuscules (409)")
    void rejectsDuplicateEmail() throws Exception {
        registerAndLogin("alice@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_ALICE.replace("alice@example.com", "Alice@Example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("refuse un mauvais mot de passe (401)")
    void rejectsWrongPassword() throws Exception {
        registerAndLogin("alice@example.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "alice@example.com", "password": "mauvais-mot-de-passe"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("les salles sont consultables sans être connecté")
    void roomsArePublic() throws Exception {
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    @DisplayName("réserver sans jeton est refusé (401)")
    void bookingRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(ASILE_ID, nextWeekAt(11, 30), 4)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un client n'a pas accès aux routes admin (403)")
    void customerCannotAccessAdminRoutes() throws Exception {
        String token = registerAndLogin("alice@example.com");

        mockMvc.perform(get("/api/admin/bookings")
                        .param("date", nextWeekAt(10, 0).toLocalDate().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }
}