package com.rutamotor.inventory.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ReservationRequest(UUID reference, VehicleId vehicleId, Buyer buyer) {
  public ReservationRequest {
    Objects.requireNonNull(reference);
    Objects.requireNonNull(vehicleId);
    Objects.requireNonNull(buyer);
  }
}
