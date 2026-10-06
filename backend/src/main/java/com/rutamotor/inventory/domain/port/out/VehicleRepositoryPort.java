package com.rutamotor.inventory.domain.port.out;

import com.rutamotor.inventory.domain.model.*;
import java.util.Optional;

public interface VehicleRepositoryPort {
  Optional<Vehicle> find(VehicleId id);

  CatalogPage search(String brand, int page, int size);

  void save(Vehicle vehicle);
}
