package com.portfolio.hotel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.hotel.reservation.dto.CreateReservationRequest;
import com.portfolio.hotel.reservation.dto.UpdateReservationRequest;
import com.portfolio.hotel.frontoffice.dto.ExtendStayRequest;
import com.portfolio.hotel.frontoffice.service.FrontOfficeService;
import com.portfolio.hotel.reservation.repository.ReservationCommandRepository;
import com.portfolio.hotel.reservation.service.ReservationService;
import com.portfolio.hotel.room.service.RoomService;
import com.portfolio.hotel.shared.error.BusinessValidationException;
import com.portfolio.hotel.shared.error.ReservationConflictException;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class HotelApiOracleIntegrationTest {

    private static final AtomicInteger DATE_SEQUENCE = new AtomicInteger();
    private static final AtomicInteger ROOM_SEQUENCE = new AtomicInteger();

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ReservationService reservationService;
    @Autowired ReservationCommandRepository commandRepository;
    @Autowired FrontOfficeService frontOfficeService;
    @Autowired RoomService roomService;
    @Autowired PlatformTransactionManager transactionManager;

    private final List<Long> committedReservationIds = new CopyOnWriteArrayList<>();
    private final List<Long> committedRoomIds = new CopyOnWriteArrayList<>();
    private final AtomicLong committedClientId = new AtomicLong();

    @AfterEach
    void removeOnlyCommittedTestReservation() {
        for (long id : committedReservationIds) {
            jdbcTemplate.update("UPDATE Reservation SET statut = 'annulée' WHERE id_res = ?", id);
            jdbcTemplate.update("DELETE FROM PersonneReservation WHERE id_res = ?", id);
            jdbcTemplate.update("DELETE FROM Reservation WHERE id_res = ?", id);
        }
        committedReservationIds.clear();
        for (long roomId : committedRoomIds) {
            jdbcTemplate.update("DELETE FROM Chambre WHERE id_chambre = ?", roomId);
        }
        committedRoomIds.clear();
        long clientId = committedClientId.getAndSet(0);
        if (clientId > 0) {
            jdbcTemplate.update("DELETE FROM Client WHERE id_client = ?", clientId);
        }
    }

    @Test
    void searchesAvailabilityAndClientsAgainstOracle() throws Exception {
        LocalDate start = nextStartDate();

        mockMvc.perform(get("/api/v1/rooms/availability")
                        .param("checkIn", start.toString())
                        .param("checkOut", start.plusDays(2).toString())
                        .param("adults", "1")
                        .param("children", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].capacity").isNumber());

        mockMvc.perform(get("/api/v1/clients")
                        .param("query", "Amine")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("amine.demo@example.test"));
    }

    @Test
    @Transactional
    void createsAndReadsACompleteReservation() throws Exception {
        CreateReservationRequest request = request(clientId("amine.demo@example.test"), roomId("301"), nextStartDate(), 2, 1);

        String body = mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("confirmée"))
                .andExpect(jsonPath("$.occupants.adults").value(2))
                .andExpect(jsonPath("$.occupants.children").value(1))
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(body).get("id").asLong();
        mockMvc.perform(get("/api/v1/reservations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.room.number").value("301"));
    }

    @Test
    @Transactional
    void createsClientThenReservationAndFindsItInCatalogAndList() throws Exception {
        String email = "mission6." + System.nanoTime() + "@example.test";
        String clientBody = mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "firstName", "Lina",
                                "lastName", "El Mansouri",
                                "email", email,
                                "phone", "+212600000099"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andReturn().getResponse().getContentAsString();
        long newClientId = objectMapper.readTree(clientBody).get("id").asLong();

        mockMvc.perform(get("/api/v1/rooms")
                        .param("query", "301")
                        .param("minCapacity", "3")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].number").value("301"))
                .andExpect(jsonPath("$.content[0].operationalStatus").value("en_service"))
                .andExpect(jsonPath("$.content[0].housekeepingStatus").value("prete"));

        LocalDate start = nextStartDate();
        String reservationBody = mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request(
                                newClientId, roomId("301"), start, 2, 1))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long reservationId = objectMapper.readTree(reservationBody).get("id").asLong();

        mockMvc.perform(get("/api/v1/reservations")
                        .param("query", email)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(reservationId))
                .andExpect(jsonPath("$.content[0].client.email").value(email));

        mockMvc.perform(get("/api/v1/reservations/{id}", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.client.id").value(newClientId));
    }

    @Test
    void rejectsDuplicateClientEmailAndCleansUp() throws Exception {
        String email = "duplicate." + System.nanoTime() + "@example.test";
        byte[] payload = objectMapper.writeValueAsBytes(Map.of(
                "firstName", "Nora", "lastName", "Bennani", "email", email
        ));
        String body = mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        committedClientId.set(objectMapper.readTree(body).get("id").asLong());

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));
    }

    @Test
    @Transactional
    void updatesDatesRoomAndOccupantsAndExposesCrossMonthPlanning() throws Exception {
        LocalDate adjacentStart = LocalDate.of(2050, 1, 28);
        long client = clientId("amine.demo@example.test");
        long targetRoom = roomId("102");
        reservationService.create(request(client, targetRoom, adjacentStart, 1, 0));
        long reservationId = reservationService.create(request(
                clientId("sara.demo@example.test"), roomId("301"), LocalDate.of(2050, 2, 10), 1, 0)).id();

        UpdateReservationRequest update = new UpdateReservationRequest(
                targetRoom, LocalDate.of(2050, 1, 31), LocalDate.of(2050, 2, 3), 2, 0);
        mockMvc.perform(patch("/api/v1/reservations/{id}", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.room.number").value("102"))
                .andExpect(jsonPath("$.checkIn").value("2050-01-31"))
                .andExpect(jsonPath("$.occupants.adults").value(2));

        mockMvc.perform(get("/api/v1/planning")
                        .param("start", "2050-01-29")
                        .param("end", "2050-02-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.start").value("2050-01-29"))
                .andExpect(jsonPath("$.rooms[?(@.number == '102')].reservations[?(@.id == " + reservationId + ")].checkOut").exists());
    }

    @Test
    @Transactional
    void rejectsOverlappingUpdateWithoutChangingReservation() throws Exception {
        LocalDate start = LocalDate.of(2051, 4, 10);
        long client = clientId("amine.demo@example.test");
        long occupiedRoom = roomId("102");
        reservationService.create(request(client, occupiedRoom, start, 1, 0));
        var subject = reservationService.create(request(client, roomId("301"), start.plusDays(10), 1, 0));

        mockMvc.perform(patch("/api/v1/reservations/{id}", subject.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new UpdateReservationRequest(
                                occupiedRoom, start.plusDays(1), start.plusDays(2), 1, 0))))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/reservations/{id}", subject.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.room.number").value("301"))
                .andExpect(jsonPath("$.checkIn").value(start.plusDays(10).toString()));
    }

    @Test
    void rollsBackCompleteReservationUpdateWhenTransactionFails() {
        LocalDate start = LocalDate.of(2054, 8, 10);
        var original = reservationService.create(request(
                clientId("amine.demo@example.test"), roomId("301"), start, 1, 0));
        committedReservationIds.add(original.id());
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            reservationService.update(original.id(), new UpdateReservationRequest(
                    roomId("102"), start.plusDays(2), start.plusDays(6), 2, 0));
            throw new IllegalStateException("Échec simulé après la modification.");
        })).isInstanceOf(IllegalStateException.class);

        var afterRollback = reservationService.get(original.id());
        assertThat(afterRollback.room().number()).isEqualTo("301");
        assertThat(afterRollback.checkIn()).isEqualTo(start);
        assertThat(afterRollback.occupants().adults()).isEqualTo(1);
    }

    @Test
    @Transactional
    void cancelsIdempotentlyAndReleasesAvailabilityAndRejectsInvalidTransition() throws Exception {
        LocalDate start = LocalDate.of(2052, 6, 1);
        long room = roomId("301");
        long id = reservationService.create(request(clientId("amine.demo@example.test"), room, start, 1, 0)).id();

        mockMvc.perform(post("/api/v1/reservations/{id}/cancel", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("annulée"));
        mockMvc.perform(post("/api/v1/reservations/{id}/cancel", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("annulée"));
        mockMvc.perform(get("/api/v1/rooms/availability")
                        .param("checkIn", start.toString())
                        .param("checkOut", start.plusDays(3).toString())
                        .param("adults", "1").param("children", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + room + ")].number").exists());
        mockMvc.perform(get("/api/v1/planning")
                        .param("start", start.toString())
                        .param("end", start.plusDays(3).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms[2].id").value(room))
                .andExpect(jsonPath("$.rooms[2].reservations").isEmpty());

        long activeId = reservationService.create(request(
                clientId("sara.demo@example.test"), room, start.plusDays(5), 1, 0)).id();
        jdbcTemplate.update("UPDATE Reservation SET statut = 'en_cours' WHERE id_res = ?", activeId);
        mockMvc.perform(post("/api/v1/reservations/{id}/cancel", activeId))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/v1/reservations/{id}", activeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new UpdateReservationRequest(
                                room, start.plusDays(6), start.plusDays(8), 1, 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void serializesConcurrentUpdatesToTheSameRoom() throws Exception {
        LocalDate original = LocalDate.of(2053, 1, 1);
        LocalDate target = LocalDate.of(2053, 3, 1);
        long client = clientId("amine.demo@example.test");
        long firstId = reservationService.create(request(client, roomId("101"), original, 1, 0)).id();
        long secondId = reservationService.create(request(
                clientId("sara.demo@example.test"), roomId("301"), original.plusDays(10), 1, 0)).id();
        committedReservationIds.add(firstId);
        committedReservationIds.add(secondId);
        long targetRoom = roomId("102");
        UpdateReservationRequest update = new UpdateReservationRequest(
                targetRoom, target, target.plusDays(3), 1, 0);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch fire = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Attempt>> futures = List.of(
                    executor.submit(() -> updateAttempt(ready, fire, firstId, update)),
                    executor.submit(() -> updateAttempt(ready, fire, secondId, update))
            );
            ready.await();
            fire.countDown();
            List<Attempt> attempts = List.of(futures.get(0).get(), futures.get(1).get());
            assertThat(attempts.stream().filter(Attempt::success)).hasSize(1);
            assertThat(attempts.stream().filter(a -> a.error() instanceof ReservationConflictException)).hasSize(1);
            assertThat(jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM Reservation
                     WHERE id_chambre = ? AND date_deb = ? AND statut = 'confirmée'
                    """, Integer.class, targetRoom, java.sql.Date.valueOf(target))).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void checksInAndOutPersistsActualTimesAndUpdatesPlanning() throws Exception {
        long room = createCommittedTestRoom();
        LocalDate today = LocalDate.now();
        long id = reservationService.create(request(
                clientId("amine.demo@example.test"), room, today, 1, 0)).id();
        committedReservationIds.add(id);

        mockMvc.perform(post("/api/v1/reservations/{id}/check-in", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("en_cours"))
                .andExpect(jsonPath("$.actualCheckIn").isNotEmpty())
                .andExpect(jsonPath("$.actualCheckOut").doesNotExist());
        mockMvc.perform(post("/api/v1/reservations/{id}/check-in", id))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/reservations/{id}/check-out", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("terminée"))
                .andExpect(jsonPath("$.actualCheckOut").isNotEmpty());
        mockMvc.perform(post("/api/v1/reservations/{id}/check-out", id))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/reservations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualCheckIn").isNotEmpty())
                .andExpect(jsonPath("$.actualCheckOut").isNotEmpty());
        mockMvc.perform(get("/api/v1/planning")
                        .param("start", today.toString())
                        .param("end", today.plusDays(3).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms[?(@.id == " + room + ")].reservations[?(@.id == " + id + ")].status").exists());
    }

    @Test
    void rejectsCheckInOutsideStayWindowAndCheckOutBeforeArrival() throws Exception {
        long room = createCommittedTestRoom();
        LocalDate future = LocalDate.now().plusDays(30);
        long id = reservationService.create(request(
                clientId("sara.demo@example.test"), room, future, 1, 0)).id();
        committedReservationIds.add(id);

        mockMvc.perform(post("/api/v1/reservations/{id}/check-in", id))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/reservations/{id}/check-out", id))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/reservations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("confirmée"))
                .andExpect(jsonPath("$.actualCheckIn").doesNotExist());
    }

    @Test
    @Transactional
    void extendsStayWhenAdjacentAndRejectsConflict() throws Exception {
        long room = createTransactionalTestRoom();
        LocalDate start = LocalDate.of(2060, 1, 28);
        long client = clientId("amine.demo@example.test");
        long id = reservationService.create(request(client, room, start, 1, 0)).id();
        reservationService.create(request(
                clientId("sara.demo@example.test"), room, start.plusDays(5), 1, 0));

        mockMvc.perform(patch("/api/v1/reservations/{id}/extend", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ExtendStayRequest(start.plusDays(5)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkOut").value(start.plusDays(5).toString()));
        mockMvc.perform(patch("/api/v1/reservations/{id}/extend", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ExtendStayRequest(start.plusDays(6)))))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/reservations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkOut").value(start.plusDays(5).toString()));
    }

    @Test
    void rollsBackCheckInWhenOuterTransactionFails() {
        long room = createCommittedTestRoom();
        LocalDate today = LocalDate.now();
        long id = reservationService.create(request(
                clientId("amine.demo@example.test"), room, today, 1, 0)).id();
        committedReservationIds.add(id);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            frontOfficeService.checkIn(id);
            throw new IllegalStateException("Échec simulé après l'arrivée.");
        })).isInstanceOf(IllegalStateException.class);

        var reloaded = reservationService.get(id);
        assertThat(reloaded.status()).isEqualTo("confirmée");
        assertThat(reloaded.actualCheckIn()).isNull();
    }

    @Test
    void serializesConcurrentCheckInAttempts() throws Exception {
        long room = createCommittedTestRoom();
        LocalDate today = LocalDate.now();
        long id = reservationService.create(request(
                clientId("amine.demo@example.test"), room, today, 1, 0)).id();
        committedReservationIds.add(id);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch fire = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Attempt>> futures = List.of(
                    executor.submit(() -> frontOfficeAttempt(ready, fire, id)),
                    executor.submit(() -> frontOfficeAttempt(ready, fire, id))
            );
            ready.await();
            fire.countDown();
            List<Attempt> attempts = List.of(futures.get(0).get(), futures.get(1).get());
            assertThat(attempts.stream().filter(Attempt::success)).hasSize(1);
            assertThat(attempts.stream().filter(a -> a.error() instanceof com.portfolio.hotel.shared.error.BusinessValidationException)).hasSize(1);
            assertThat(reservationService.get(id).status()).isEqualTo("en_cours");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void checkoutStartsHousekeepingCycleAndReadyRoomAcceptsNextCheckIn() throws Exception {
        long room = createCommittedTestRoom();
        LocalDate today = LocalDate.now();
        long firstId = reservationService.create(request(
                clientId("amine.demo@example.test"), room, today, 1, 0)).id();
        committedReservationIds.add(firstId);
        frontOfficeService.checkIn(firstId);
        frontOfficeService.checkOut(firstId);

        assertThat(roomState(room, "etat_proprete")).isEqualTo("a_nettoyer");
        long nextId = reservationService.create(request(
                clientId("sara.demo@example.test"), room, today, 1, 0)).id();
        committedReservationIds.add(nextId);
        assertThatThrownBy(() -> frontOfficeService.checkIn(nextId))
                .isInstanceOf(ReservationConflictException.class);

        mockMvc.perform(patch("/api/v1/rooms/{id}/housekeeping", room)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("status", "nettoyee"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.housekeepingStatus").value("nettoyee"));
        mockMvc.perform(patch("/api/v1/rooms/{id}/housekeeping", room)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("status", "prete"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.housekeepingStatus").value("prete"));
        assertThat(frontOfficeService.checkIn(nextId).status()).isEqualTo("en_cours");
    }

    @Test
    void togglesMaintenanceAndKeepsFutureAvailabilityCoherent() throws Exception {
        long room = createCommittedTestRoom();
        LocalDate future = LocalDate.now().plusDays(90);

        mockMvc.perform(patch("/api/v1/rooms/{id}/operational-status", room)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("status", "hors_service"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationalStatus").value("hors_service"));
        mockMvc.perform(get("/api/v1/rooms/availability")
                        .param("checkIn", future.toString())
                        .param("checkOut", future.plusDays(3).toString())
                        .param("adults", "1").param("children", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + room + ")]").isEmpty());
        assertThatThrownBy(() -> reservationService.create(request(
                clientId("amine.demo@example.test"), room, future, 1, 0)))
                .isInstanceOf(ReservationConflictException.class);

        roomService.updateOperationalStatus(room, "en_service");
        assertThat(roomState(room, "etat_operationnel")).isEqualTo("en_service");
        assertThat(roomState(room, "statut")).isEqualTo("libre");
    }

    @Test
    void rejectsMaintenanceWithFutureOrActiveStayAndRejectsInvalidCleaningTransitions() {
        long room = createCommittedTestRoom();
        LocalDate future = LocalDate.now().plusDays(30);
        long futureId = reservationService.create(request(
                clientId("amine.demo@example.test"), room, future, 1, 0)).id();
        committedReservationIds.add(futureId);

        assertThatThrownBy(() -> roomService.updateOperationalStatus(room, "hors_service"))
                .isInstanceOf(ReservationConflictException.class);
        assertThat(roomState(room, "etat_operationnel")).isEqualTo("en_service");
        assertThatThrownBy(() -> roomService.updateHousekeeping(room, "nettoyee"))
                .isInstanceOf(BusinessValidationException.class);
        jdbcTemplate.update("UPDATE Chambre SET etat_proprete = 'a_nettoyer' WHERE id_chambre = ?", room);
        assertThatThrownBy(() -> roomService.updateHousekeeping(room, "prete"))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void rollsBackCheckoutAndHousekeepingTogether() {
        long room = createCommittedTestRoom();
        LocalDate today = LocalDate.now();
        long id = reservationService.create(request(
                clientId("amine.demo@example.test"), room, today, 1, 0)).id();
        committedReservationIds.add(id);
        frontOfficeService.checkIn(id);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            frontOfficeService.checkOut(id);
            throw new IllegalStateException("Échec simulé après le départ.");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(reservationService.get(id).status()).isEqualTo("en_cours");
        assertThat(reservationService.get(id).actualCheckOut()).isNull();
        assertThat(roomState(room, "etat_proprete")).isEqualTo("prete");
    }

    @Test
    void serializesMaintenanceAgainstReservationCreation() throws Exception {
        long room = createCommittedTestRoom();
        LocalDate future = LocalDate.now().plusDays(120);
        CreateReservationRequest request = request(
                clientId("amine.demo@example.test"), room, future, 1, 0);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch fire = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Attempt> maintenance = executor.submit(() -> maintenanceAttempt(ready, fire, room));
            Future<Attempt> reservation = executor.submit(() -> attempt(ready, fire, request));
            ready.await();
            fire.countDown();
            List<Attempt> attempts = List.of(maintenance.get(), reservation.get());
            assertThat(attempts.stream().filter(Attempt::success)).hasSize(1);
            assertThat(attempts.stream().filter(a -> a.error() instanceof ReservationConflictException)).hasSize(1);
            attempts.stream().filter(Attempt::success).map(Attempt::reservationId)
                    .filter(id -> id > 0).forEach(committedReservationIds::add);
        } finally {
            executor.shutdownNow();
            if ("hors_service".equals(roomState(room, "etat_operationnel"))) {
                roomService.updateOperationalStatus(room, "en_service");
            }
        }
    }

    @Test
    @Transactional
    void rejectsInvalidDatesCapacityAndOverlap() throws Exception {
        LocalDate start = nextStartDate();
        long clientId = clientId("amine.demo@example.test");
        long smallRoomId = roomId("102");

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new CreateReservationRequest(
                                clientId, smallRoomId, start, start, 1, 0))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request(clientId, smallRoomId, start, 3, 0))))
                .andExpect(status().isBadRequest());

        long roomId = roomId("301");
        reservationService.create(request(clientId, roomId, start, 1, 0));
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request(
                                clientId("sara.demo@example.test"), roomId, start.plusDays(1), 1, 0))))
                .andExpect(status().isConflict());
    }

    @Test
    void rollsBackThePackageWorkWhenALaterStepFails() {
        LocalDate start = nextStartDate();
        CreateReservationRequest request = request(clientId("amine.demo@example.test"), roomId("301"), start, 1, 0);
        AtomicLong createdId = new AtomicLong();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            long id = commandRepository.createConfirmed(request);
            createdId.set(id);
            long adultType = jdbcTemplate.queryForObject(
                    "SELECT id_type_personne FROM TypePersonne WHERE libelle = 'adulte'", Long.class);
            jdbcTemplate.update(
                    "INSERT INTO PersonneReservation (id_res, id_type_personne, quantite) VALUES (?, ?, ?)",
                    id, adultType, 1);
        })).isInstanceOf(DataIntegrityViolationException.class);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Reservation WHERE id_res = ?", Integer.class, createdId.get());
        assertThat(count).isZero();
    }

    @Test
    void allowsOnlyOneOfTwoConcurrentReservations() throws Exception {
        LocalDate start = nextStartDate();
        long roomId = roomId("301");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch fire = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Attempt>> futures = List.of(
                    executor.submit(() -> attempt(ready, fire, request(clientId("amine.demo@example.test"), roomId, start, 1, 0))),
                    executor.submit(() -> attempt(ready, fire, request(clientId("sara.demo@example.test"), roomId, start, 1, 0)))
            );
            ready.await();
            fire.countDown();
            List<Attempt> attempts = List.of(futures.get(0).get(), futures.get(1).get());

            assertThat(attempts.stream().filter(Attempt::success)).hasSize(1);
            assertThat(attempts.stream().filter(a -> a.error() instanceof ReservationConflictException)).hasSize(1);
            long id = attempts.stream().filter(Attempt::success).findFirst().orElseThrow().reservationId();
            committedReservationIds.add(id);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM Reservation WHERE id_res = ?", Integer.class, id)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private Attempt attempt(CountDownLatch ready, CountDownLatch fire, CreateReservationRequest request) {
        ready.countDown();
        try {
            fire.await();
            return new Attempt(true, reservationService.create(request).id(), null);
        } catch (Throwable error) {
            return new Attempt(false, 0, error);
        }
    }

    private Attempt updateAttempt(
            CountDownLatch ready, CountDownLatch fire, long id, UpdateReservationRequest request
    ) {
        ready.countDown();
        try {
            fire.await();
            return new Attempt(true, reservationService.update(id, request).id(), null);
        } catch (Throwable error) {
            return new Attempt(false, 0, error);
        }
    }

    private Attempt frontOfficeAttempt(CountDownLatch ready, CountDownLatch fire, long id) {
        ready.countDown();
        try {
            fire.await();
            return new Attempt(true, frontOfficeService.checkIn(id).id(), null);
        } catch (Throwable error) {
            return new Attempt(false, 0, error);
        }
    }

    private Attempt maintenanceAttempt(CountDownLatch ready, CountDownLatch fire, long roomId) {
        ready.countDown();
        try {
            fire.await();
            roomService.updateOperationalStatus(roomId, "hors_service");
            return new Attempt(true, 0, null);
        } catch (Throwable error) {
            return new Attempt(false, 0, error);
        }
    }

    private String roomState(long roomId, String column) {
        if (!Set.of("etat_operationnel", "etat_proprete", "statut").contains(column)) {
            throw new IllegalArgumentException("Colonne de test non autorisée.");
        }
        return jdbcTemplate.queryForObject(
                "SELECT " + column + " FROM Chambre WHERE id_chambre = ?", String.class, roomId);
    }

    private CreateReservationRequest request(long clientId, long roomId, LocalDate start, int adults, int children) {
        return new CreateReservationRequest(clientId, roomId, start, start.plusDays(3), adults, children);
    }

    private long clientId(String email) {
        return jdbcTemplate.queryForObject("SELECT id_client FROM Client WHERE email = ?", Long.class, email);
    }

    private long roomId(String number) {
        return jdbcTemplate.queryForObject("SELECT id_chambre FROM Chambre WHERE numero = ?", Long.class, number);
    }

    private long createCommittedTestRoom() {
        String number = "T" + ROOM_SEQUENCE.incrementAndGet();
        jdbcTemplate.update("""
                INSERT INTO Chambre(numero, etage, statut, id_type)
                VALUES (?, 9, 'libre', (SELECT id_type FROM TypeChambre WHERE libelle = 'Double'))
                """, number);
        long id = roomId(number);
        committedRoomIds.add(id);
        return id;
    }

    private long createTransactionalTestRoom() {
        String number = "X" + ROOM_SEQUENCE.incrementAndGet();
        jdbcTemplate.update("""
                INSERT INTO Chambre(numero, etage, statut, id_type)
                VALUES (?, 9, 'libre', (SELECT id_type FROM TypeChambre WHERE libelle = 'Double'))
                """, number);
        return roomId(number);
    }

    private LocalDate nextStartDate() {
        return LocalDate.of(2040, 1, 1).plusDays(DATE_SEQUENCE.getAndAdd(7));
    }

    private record Attempt(boolean success, long reservationId, Throwable error) {
    }
}
