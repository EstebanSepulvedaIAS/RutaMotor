package com.rutamotor.inventory.domain.port.out;

import com.rutamotor.inventory.domain.model.*;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepositoryPort {
  Optional<ReservationResult> find(UUID reference);

  void save(ReservationResult result);
}
