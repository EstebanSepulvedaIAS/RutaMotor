package com.rutamotor.inventory.infrastructure.adapter.out.persistence;

import com.rutamotor.inventory.domain.model.*;

public final class PersistenceMapper {
  private PersistenceMapper() {}

  public static Vehicle domain(VehicleJpaEntity e) {
    return new Vehicle(
        new VehicleId(e.id),
        new Vin(e.vin),
        e.brand,
        e.model,
        new Money(e.price),
        VehicleStatus.valueOf(e.status));
  }

  public static void copy(Vehicle v, VehicleJpaEntity e) {
    e.id = v.id().value();
    e.vin = v.vin().value();
    e.brand = v.brand();
    e.model = v.model();
    e.price = v.price().amount();
    e.status = v.status().name();
  }

  public static ReservationResult domain(ResultJpaEntity e) {
    return new ReservationResult(
        new ReservationRequest(e.reference, new VehicleId(e.vehicleId), new Buyer(e.buyerAlias)),
        e.processedAt,
        ReservationResult.Outcome.valueOf(e.outcome),
        e.reason == null ? null : ReservationResult.Reason.valueOf(e.reason),
        e.appliedPrice == null ? null : new Money(e.appliedPrice));
  }

  public static ResultJpaEntity entity(ReservationResult r) {
    var e = new ResultJpaEntity();
    e.reference = r.request().reference();
    e.vehicleId = r.request().vehicleId().value();
    e.buyerAlias = r.request().buyer().alias();
    e.processedAt = r.processedAt();
    e.outcome = r.outcome().name();
    e.reason = r.reason() == null ? null : r.reason().name();
    e.appliedPrice = r.appliedPrice() == null ? null : r.appliedPrice().amount();
    return e;
  }
}
