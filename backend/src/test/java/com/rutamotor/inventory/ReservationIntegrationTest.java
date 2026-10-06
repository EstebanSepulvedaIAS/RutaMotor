package com.rutamotor.inventory;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rutamotor.inventory.domain.model.*;
import com.rutamotor.inventory.domain.port.in.ReserveVehicleCommand;
import com.rutamotor.inventory.domain.port.out.VehicleRepositoryPort;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:integration;DB_CLOSE_DELAY=-1",
      "rutamotor.seed-size=10000"
    })
@AutoConfigureMockMvc
class ReservationIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired ReserveVehicleCommand reserve;
  @Autowired VehicleRepositoryPort vehicles;
  @Autowired PlatformTransactionManager manager;

  private UUID unit() {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "insert into vehicles(id,vin,brand,model,price,status,version)"
            + " values(?,?,?,?,?,'AVAILABLE',0)",
        id,
        "RM" + id.toString().replace("-", "").substring(0, 15).toUpperCase(),
        "Test",
        "Unit",
        123456789.00);
    return id;
  }

  private ReservationRequest request(UUID id, UUID ref, String alias) {
    return new ReservationRequest(ref, new VehicleId(id), new Buyer(alias));
  }

  private String payload(UUID id, UUID ref, String alias) throws Exception {
    return json.writeValueAsString(Map.of("vehicleId", id, "reference", ref, "buyerAlias", alias));
  }

  @Test
  void paginatesTenThousandWithoutSendingAllRows() throws Exception {
    var response =
        mvc.perform(get("/api/v1/vehicles").param("size", "25").param("brand", "Toyota"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(25))
            .andReturn()
            .getResponse();
    assertThat(response.getContentAsByteArray().length).isLessThan(15000);
    mvc.perform(get("/api/v1/vehicles").param("size", "26")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/vehicles").param("page", "-1")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/vehicles").param("page", "oops")).andExpect(status().isBadRequest());
  }

  @Test
  void acceptsReplaysAndRejectsDifferentBuyer() throws Exception {
    var id = unit();
    var ref = UUID.randomUUID();
    var body = payload(id, ref, "Demo-01");
    String first =
        mvc.perform(
                post("/api/v1/reservations").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.appliedPrice").value(123456789.00))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String replay =
        mvc.perform(
                post("/api/v1/reservations").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(json.readTree(replay)).isEqualTo(json.readTree(first));
    mvc.perform(
            post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(id, ref, "Demo-02")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Referencia en conflicto"));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from reservation_results where reference=?", Integer.class, ref))
        .isEqualTo(1);
  }

  @Test
  void missingRejectionRemainsOriginalEvenIfUnitAppearsLater() throws Exception {
    var id = UUID.randomUUID();
    var ref = UUID.randomUUID();
    var body = payload(id, ref, "Demo-01");
    String first =
        mvc.perform(
                post("/api/v1/reservations").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.result.reason").value("NOT_FOUND"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    jdbc.update(
        "insert into vehicles(id,vin,brand,model,price,status,version)"
            + " values(?,?,'Test','Late',100,'AVAILABLE',0)",
        id,
        "RM" + id.toString().replace("-", "").substring(0, 15).toUpperCase());
    String replay =
        mvc.perform(
                post("/api/v1/reservations").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isNotFound())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(json.readTree(replay)).isEqualTo(json.readTree(first));
    assertThat(jdbc.queryForObject("select status from vehicles where id=?", String.class, id))
        .isEqualTo("AVAILABLE");
  }

  @Test
  void rejectsInvalidDataAndClientPriceWithoutRecording() throws Exception {
    var id = unit();
    var ref = UUID.randomUUID();
    mvc.perform(
            post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(id, ref, " ")))
        .andExpect(status().isBadRequest());
    var withPrice = json.readTree(payload(id, ref, "Demo-01"));
    ((com.fasterxml.jackson.databind.node.ObjectNode) withPrice).put("price", 1);
    mvc.perform(
            post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(withPrice.toString()))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid"))
        .andExpect(status().isBadRequest());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from reservation_results where reference=?", Integer.class, ref))
        .isZero();
  }

  @Test
  void concurrentDifferentReferencesYieldOneAcceptanceAndDurableRejections() throws Exception {
    var id = unit();
    var start = new CountDownLatch(1);
    var requests = new ArrayList<ReservationRequest>();
    try (var pool = Executors.newFixedThreadPool(8)) {
      var futures = new ArrayList<Future<ReservationResult>>();
      for (int i = 0; i < 8; i++) {
        var r = request(id, UUID.randomUUID(), "Buyer-" + i);
        requests.add(r);
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  return reserve.reserve(r);
                }));
      }
      start.countDown();
      int accepted = 0;
      for (var future : futures)
        if (future.get(20, TimeUnit.SECONDS).outcome() == ReservationResult.Outcome.ACCEPTED)
          accepted++;
      assertThat(accepted).isEqualTo(1);
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from reservation_results where vehicle_id=?", Integer.class, id))
          .isEqualTo(8);
      assertThat(jdbc.queryForObject("select version from vehicles where id=?", Integer.class, id))
          .isEqualTo(1);
      for (var r : requests) {
        var original = reserve.reserve(r);
        assertThat(reserve.reserve(r)).isEqualTo(original);
      }
    }
  }

  @Test
  void concurrentSameReferenceReturnsIdenticalResult() throws Exception {
    var id = unit();
    var r = request(id, UUID.randomUUID(), "Same-buyer");
    var start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(4)) {
      var futures = new ArrayList<Future<ReservationResult>>();
      for (int i = 0; i < 4; i++)
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  return reserve.reserve(r);
                }));
      start.countDown();
      var first = futures.getFirst().get(20, TimeUnit.SECONDS);
      for (var future : futures) assertThat(future.get(20, TimeUnit.SECONDS)).isEqualTo(first);
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from reservation_results where reference=?",
                  Integer.class,
                  r.reference()))
          .isEqualTo(1);
      assertThat(jdbc.queryForObject("select version from vehicles where id=?", Integer.class, id))
          .isEqualTo(1);
    }
  }

  @Test
  void reusedReferenceAcrossConcurrentUnitsRollsBackLosingUnit() throws Exception {
    var first = unit();
    var second = unit();
    var ref = UUID.randomUUID();
    var start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var futures = new ArrayList<Future<Boolean>>();
      for (var id : List.of(first, second))
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  try {
                    reserve.reserve(request(id, ref, "Buyer-01"));
                    return true;
                  } catch (com.rutamotor.inventory.domain.exception.ReferenceConflict e) {
                    return false;
                  }
                }));
      start.countDown();
      int accepted = 0;
      for (var f : futures) if (f.get(20, TimeUnit.SECONDS)) accepted++;
      assertThat(accepted).isEqualTo(1);
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from vehicles where id in (?,?) and status='RESERVED'",
                  Integer.class,
                  first,
                  second))
          .isEqualTo(1);
    }
  }

  @Test
  void optimisticVersionActuallyDetectsTwoStaleWriters() throws Exception {
    var id = unit();
    var barrier = new CyclicBarrier(2);
    var successes = new AtomicInteger();
    var collisions = new AtomicInteger();
    try (var pool = Executors.newFixedThreadPool(2)) {
      var jobs = new ArrayList<Future<?>>();
      for (int i = 0; i < 2; i++)
        jobs.add(
            pool.submit(
                () -> {
                  try {
                    new TransactionTemplate(manager)
                        .executeWithoutResult(
                            tx -> {
                              var v = vehicles.find(new VehicleId(id)).orElseThrow();
                              try {
                                barrier.await(10, TimeUnit.SECONDS);
                              } catch (Exception e) {
                                throw new RuntimeException(e);
                              }
                              v.reserveFor(new Buyer("Test-buyer"));
                              vehicles.save(v);
                            });
                    successes.incrementAndGet();
                  } catch (OptimisticLockingFailureException e) {
                    collisions.incrementAndGet();
                  }
                }));
      for (var job : jobs) job.get(20, TimeUnit.SECONDS);
      assertThat(successes.get()).isEqualTo(1);
      assertThat(collisions.get()).isEqualTo(1);
    }
  }

  @Test
  void differentUnitProgressesWhileAnotherUnitIsHeld() throws Exception {
    var held = unit();
    var free = unit();
    var locked = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var holder =
          pool.submit(
              () ->
                  new TransactionTemplate(manager)
                      .executeWithoutResult(
                          tx -> {
                            var v = vehicles.find(new VehicleId(held)).orElseThrow();
                            v.reserveFor(new Buyer("Held-buyer"));
                            vehicles.save(v);
                            locked.countDown();
                            try {
                              release.await(10, TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                              Thread.currentThread().interrupt();
                              throw new RuntimeException(e);
                            }
                          }));
      try {
        assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
        var result =
            pool.submit(() -> reserve.reserve(request(free, UUID.randomUUID(), "Free-buyer")))
                .get(5, TimeUnit.SECONDS);
        assertThat(result.outcome()).isEqualTo(ReservationResult.Outcome.ACCEPTED);
      } finally {
        release.countDown();
      }
      holder.get(10, TimeUnit.SECONDS);
    }
  }
}
