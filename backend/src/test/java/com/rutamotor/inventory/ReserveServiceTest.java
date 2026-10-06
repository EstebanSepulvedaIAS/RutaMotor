package com.rutamotor.inventory;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.rutamotor.inventory.application.service.*;
import com.rutamotor.inventory.domain.exception.ReferenceConflict;
import com.rutamotor.inventory.domain.model.*;
import com.rutamotor.inventory.domain.port.out.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ReserveServiceTest {
  final VehicleRepositoryPort vehicles = mock(VehicleRepositoryPort.class);
  final ReservationRepositoryPort results = mock(ReservationRepositoryPort.class);
  final Clock clock = Clock.fixed(Instant.parse("2026-10-06T12:00:00Z"), ZoneOffset.UTC);
  final ReserveVehicleService service = new ReserveVehicleService(vehicles, results, clock);
  final ReservationRequest request =
      new ReservationRequest(
          UUID.randomUUID(), new VehicleId(UUID.randomUUID()), new Buyer("Demo-01"));

  @Test
  void preservesMissingVehicleRejection() {
    when(results.find(request.reference())).thenReturn(Optional.empty());
    when(vehicles.find(request.vehicleId())).thenReturn(Optional.empty());
    var result = service.reserve(request);
    assertThat(result.reason()).isEqualTo(ReservationResult.Reason.NOT_FOUND);
    verify(results).save(result);
    verify(vehicles, never()).save(any());
  }

  @Test
  void replaysWithoutTouchingVehicle() {
    var previous =
        ReservationResult.rejected(request, clock.instant(), ReservationResult.Reason.UNAVAILABLE);
    when(results.find(request.reference())).thenReturn(Optional.of(previous));
    assertThat(service.reserve(request)).isSameAs(previous);
    verifyNoInteractions(vehicles);
    verify(results, never()).save(any());
  }

  @Test
  void rejectsReusedReferenceWithoutOverwriting() {
    when(results.find(request.reference()))
        .thenReturn(
            Optional.of(
                ReservationResult.rejected(
                    request, clock.instant(), ReservationResult.Reason.NOT_FOUND)));
    var changed =
        new ReservationRequest(request.reference(), request.vehicleId(), new Buyer("Demo-02"));
    assertThatThrownBy(() -> service.reserve(changed)).isInstanceOf(ReferenceConflict.class);
    verifyNoInteractions(vehicles);
    verify(results, never()).save(any());
  }
}
