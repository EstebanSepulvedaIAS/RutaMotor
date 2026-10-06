package com.rutamotor.inventory.application.service;

import com.rutamotor.inventory.domain.exception.ReferenceConflict;
import com.rutamotor.inventory.domain.model.*;
import com.rutamotor.inventory.domain.port.in.ReserveVehicleCommand;
import com.rutamotor.inventory.domain.port.out.*;
import java.time.Clock;

public class ReserveVehicleService implements ReserveVehicleCommand {
  private final VehicleRepositoryPort vehicles;
  private final ReservationRepositoryPort results;
  private final Clock clock;

  public ReserveVehicleService(VehicleRepositoryPort v, ReservationRepositoryPort r, Clock c) {
    vehicles = v;
    results = r;
    clock = c;
  }

  public ReservationResult reserve(ReservationRequest request) {
    var previous = results.find(request.reference());
    if (previous.isPresent()) {
      if (!previous.get().request().equals(request)) throw new ReferenceConflict();
      return previous.get();
    }
    var vehicle = vehicles.find(request.vehicleId());
    var now = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    ReservationResult result;
    if (vehicle.isEmpty())
      result = ReservationResult.rejected(request, now, ReservationResult.Reason.NOT_FOUND);
    else if (!vehicle.get().reserveFor(request.buyer()))
      result = ReservationResult.rejected(request, now, ReservationResult.Reason.UNAVAILABLE);
    else {
      vehicles.save(vehicle.get());
      result = ReservationResult.accepted(request, now, vehicle.get().price());
    }
    results.save(result);
    return result;
  }
}
