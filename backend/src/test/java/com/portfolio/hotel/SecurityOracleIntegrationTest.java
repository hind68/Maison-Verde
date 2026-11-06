package com.portfolio.hotel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.hotel.security.HotelUserPrincipal;
import com.portfolio.hotel.shared.error.ResourceConflictException;
import com.portfolio.hotel.user.domain.StaffRole;
import com.portfolio.hotel.user.dto.CreateUserRequest;
import com.portfolio.hotel.user.dto.UpdateUserRequest;
import com.portfolio.hotel.user.repository.StaffAccountRepository;
import com.portfolio.hotel.user.service.UserAdministrationService;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityOracleIntegrationTest {

    private static final String PASSWORD = "Mission10!Secure123";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired UserAdministrationService administrationService;
    @Autowired StaffAccountRepository accountRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private Long testRoomId;

    @BeforeEach
    @AfterEach
    void cleanOnlySecurityFixtures() {
        if (testRoomId != null) {
            jdbcTemplate.update("DELETE FROM Chambre WHERE id_chambre = ?", testRoomId);
            testRoomId = null;
        }
        jdbcTemplate.update("DELETE FROM Employe WHERE login LIKE 'sec_%'");
    }

    @Test
    void authenticatesWithBcryptPersistsSessionAndInvalidatesLogout() throws Exception {
        var account = createUser(StaffRole.RECEPTION, "session");
        String hash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM Employe WHERE id_employe = ?", String.class, account.id());
        assertThat(hash).startsWith("$2");
        assertThat(passwordEncoder.matches(PASSWORD, hash)).isTrue();

        var login = mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "identifier", account.login(), "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("RECEPTION"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(account.login()));
        mockMvc.perform(post("/api/v1/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void requiresAndPersistsInitialPasswordChange() throws Exception {
        var account = createUser(StaffRole.RECEPTION, "password");
        var login = mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "identifier", account.login(), "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/api/v1/reservations").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));

        String replacement = "Mission10!Changed456";
        mockMvc.perform(post("/api/v1/auth/change-password").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "currentPassword", PASSWORD, "newPassword", replacement))))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "identifier", account.login(), "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "identifier", account.login(), "password", replacement))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    @Test
    void rejectsInvalidDisabledAndRepeatedCredentials() throws Exception {
        var account = createUser(StaffRole.RECEPTION, "locked");
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(Map.of(
                                    "identifier", account.email(), "password", "Incorrect!123"))))
                    .andExpect(status().isUnauthorized());
        }
        assertThat(jdbcTemplate.queryForObject(
                "SELECT verrouille_jusqua FROM Employe WHERE id_employe = ?", java.sql.Timestamp.class, account.id()))
                .isNotNull();
        mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "identifier", account.login(), "password", PASSWORD))))
                .andExpect(status().isUnauthorized());

        jdbcTemplate.update("UPDATE Employe SET actif = 0, verrouille_jusqua = NULL WHERE id_employe = ?", account.id());
        mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "identifier", account.login(), "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returns401ForAnonymous403ForForbiddenAndRequiresCsrf() throws Exception {
        mockMvc.perform(get("/api/v1/reservations")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/clients").with(user("house").roles("HOUSEKEEPING")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/auth/logout").with(user("reception").roles("RECEPTION")))
                .andExpect(status().isForbidden());
    }

    @Test
    void appliesHousekeepingReceptionManagerAndAdminPermissions() throws Exception {
        long room = createTestRoom();
        mockMvc.perform(patch("/api/v1/rooms/{id}/housekeeping", room)
                        .with(user("house").roles("HOUSEKEEPING")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("status", "nettoyee"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/reservations/{id}/cancel", 1)
                        .with(user("house").roles("HOUSEKEEPING")).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/rooms/{id}/housekeeping", room)
                        .with(user("desk").roles("RECEPTION")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("status", "prete"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/rooms/{id}/operational-status", room)
                        .with(user("manager").roles("MANAGER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("status", "hors_service"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users").with(user("manager").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void protectsLastAdministratorAndNormalizedUniqueness() {
        jdbcTemplate.update("""
                UPDATE Employe
                   SET actif = 0
                 WHERE id_role = (SELECT id_role FROM RoleEmploye WHERE libelle = 'ADMIN')
                """);
        var admin = createUser(StaffRole.ADMIN, "soleadmin");
        var reception = createUser(StaffRole.RECEPTION, "actor");
        HotelUserPrincipal actor = new HotelUserPrincipal(accountRepository.findById(reception.id()).orElseThrow());
        assertThatThrownBy(() -> administrationService.update(
                admin.id(), new UpdateUserRequest(StaffRole.MANAGER, true), actor))
                .isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(() -> administrationService.create(new CreateUserRequest(
                admin.login().toUpperCase(), "other@example.test", "Test", "Duplicate",
                StaffRole.RECEPTION, PASSWORD)))
                .isInstanceOf(com.portfolio.hotel.shared.error.ResourceConflictException.class);
    }

    @Test
    void exposesCsrfTokenAndAllowsOnlyConfiguredCorsOrigin() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"));
        mockMvc.perform(options("/api/v1/rooms")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
        mockMvc.perform(options("/api/v1/rooms")
                        .header(HttpHeaders.ORIGIN, "https://untrusted.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    private com.portfolio.hotel.user.dto.UserResponse createUser(StaffRole role, String suffix) {
        String key = "sec_" + suffix + "_" + SEQUENCE.incrementAndGet();
        return administrationService.create(new CreateUserRequest(
                key, key + "@example.test", "Test", "Security", role, PASSWORD));
    }

    private long createTestRoom() {
        String number = "S" + SEQUENCE.incrementAndGet();
        jdbcTemplate.update("""
                INSERT INTO Chambre(numero, etage, statut, etat_operationnel, etat_proprete, id_type)
                VALUES (?, 9, 'libre', 'en_service', 'a_nettoyer',
                        (SELECT id_type FROM TypeChambre WHERE libelle = 'Double'))
                """, number);
        testRoomId = jdbcTemplate.queryForObject(
                "SELECT id_chambre FROM Chambre WHERE numero = ?", Long.class, number);
        return testRoomId;
    }
}
