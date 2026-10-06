package com.rutamotor.inventory.domain.port.in;

import com.rutamotor.inventory.domain.model.*;

public interface ReserveVehicleCommand {
  ReservationResult reserve(ReservationRequest request);
}
