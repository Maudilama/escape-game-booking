package com.demo.escape_game_booking;

import com.demo.escape_game_booking.config.AdminProperties;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base des tests d'intégration : application complète + vrai PostgreSQL (Testcontainers).
 * Toutes les classes qui en héritent partagent le même contexte Spring et le même conteneur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    protected static final long ASILE_ID = 1L;
    protected static final String PASSWORD = "motdepasse123";
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    private AdminProperties adminProperties;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM bookings");
        jdbcTemplate.execute("DELETE FROM users WHERE role <> 'ADMIN'");
    }

    /** Le prochain samedi (dans 1 à 7 jours) : un jour ouvert, quel que soit le jour où les tests tournent. */
    protected static LocalDateTime nextSaturdayAt(int hour, int minute) {
        return upcomingAt(DayOfWeek.SATURDAY, hour, minute);
    }

    protected static LocalDateTime upcomingAt(DayOfWeek day, int hour, int minute) {
        return LocalDate.now(PARIS)
                .plusDays(7)
                .with(TemporalAdjusters.nextOrSame(day))
                .atTime(hour, minute);
    }

    protected static String bookingJson(long roomId, LocalDateTime startAt, int players) {
        return """
                {"roomId": %d, "startAt": "%s", "playerCount": %d}
                """.formatted(roomId, startAt, players);
    }

    protected String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s", "firstName": "Test", "lastName": "User"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated());

        return login(email, PASSWORD);
    }

    protected String loginAsAdmin() throws Exception {
        return login(adminProperties.email(), adminProperties.password());
    }

    protected String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.accessToken");
    }

    protected ResultActions createBooking(String token, LocalDateTime startAt) throws Exception {
        return mockMvc.perform(post("/api/bookings")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingJson(ASILE_ID, startAt, 4)));
    }

    protected static long idOf(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }
}