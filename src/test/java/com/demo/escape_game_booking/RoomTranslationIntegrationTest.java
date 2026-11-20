package com.demo.escape_game_booking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Salles traduites selon Accept-Language")
class RoomTranslationIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("sert le français sans en-tête Accept-Language")
    void servesFrenchByDefault() throws Exception {
        mockMvc.perform(get("/api/rooms/{id}", ASILE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("Hôpital psychiatrique"))
                .andExpect(header().string(HttpHeaders.CONTENT_LANGUAGE, "fr"));
    }

    @Test
    @DisplayName("traduit le thème et la description, mais jamais le nom")
    void translatesThemeAndDescriptionButNotName() throws Exception {
        mockMvc.perform(get("/api/rooms/{id}", ASILE_ID).header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("L'Asile de Saint-Ombre"))
                .andExpect(jsonPath("$.theme").value("Psychiatric hospital"))
                .andExpect(header().string(HttpHeaders.CONTENT_LANGUAGE, "en"));
    }

    @Test
    @DisplayName("reconnaît une langue régionale (es-MX → es)")
    void matchesRegionalVariant() throws Exception {
        mockMvc.perform(get("/api/rooms/{id}", ASILE_ID).header(HttpHeaders.ACCEPT_LANGUAGE, "es-MX,es;q=0.9"))
                .andExpect(jsonPath("$.theme").value("Hospital psiquiátrico"));
    }

    @Test
    @DisplayName("se rabat sur le français pour une langue non prise en charge")
    void fallsBackToFrench() throws Exception {
        mockMvc.perform(get("/api/rooms/{id}", ASILE_ID).header(HttpHeaders.ACCEPT_LANGUAGE, "de"))
                .andExpect(jsonPath("$.theme").value("Hôpital psychiatrique"))
                .andExpect(header().string(HttpHeaders.CONTENT_LANGUAGE, "fr"));
    }

    @Test
    @DisplayName("prévient les caches que la réponse dépend de la langue")
    void declaresVaryAcceptLanguage() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/rooms")).andReturn();

        assertThat(String.join(",", result.getResponse().getHeaders(HttpHeaders.VARY)))
                .contains(HttpHeaders.ACCEPT_LANGUAGE);
    }

    @ParameterizedTest(name = "chaque salle active a sa traduction en « {0} »")
    @ValueSource(strings = {"en", "es"})
    void everyRoomIsTranslated(String locale) {
        Integer missing = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM rooms r
                WHERE r.active
                  AND NOT EXISTS (SELECT 1 FROM room_translations t WHERE t.room_id = r.id AND t.locale = ?)
                """, Integer.class, locale);

        assertThat(missing).isZero();
    }
}