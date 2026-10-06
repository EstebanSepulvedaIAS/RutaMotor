package com.rutamotor.inventory.domain.model;

import java.time.Instant;

public record ReservationResult(
    ReservationRequest request,
    Instant processedAt,
    Outcome outcome,
    Reason reason,
    Money appliedPrice) {
  public enum Outcome {
    ACCEPTED,
    REJECTED
  }

  public enum Reason {
    NOT_FOUND,
    UNAVAILABLE
  }

  public static ReservationResult accepted(ReservationRequest r, Instant now, Money price) {
    return new ReservationResult(r, now, Outcome.ACCEPTED, null, price);
  }

  public static ReservationResult rejected(ReservationRequest r, Instant now, Reason reason) {
    return new ReservationResult(r, now, Outcome.REJECTED, reason, null);
  }
}
