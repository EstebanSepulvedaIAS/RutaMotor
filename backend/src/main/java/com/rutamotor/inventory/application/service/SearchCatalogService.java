package com.rutamotor.inventory.application.service;

import com.rutamotor.inventory.domain.model.*;
import com.rutamotor.inventory.domain.port.in.SearchCatalogQuery;
import com.rutamotor.inventory.domain.port.out.VehicleRepositoryPort;

public class SearchCatalogService implements SearchCatalogQuery {
  private final VehicleRepositoryPort vehicles;

  public SearchCatalogService(VehicleRepositoryPort vehicles) {
    this.vehicles = vehicles;
  }

  public CatalogPage search(String brand, int page, int size) {
    if (page < 0
        || page > 1000000
        || size < 1
        || size > 25
        || (brand != null && (brand.length() > 40 || !brand.matches("[A-Za-z0-9 -]*"))))
      throw new IllegalArgumentException(
          "Página: 0 a 1000000; tamaño: 1 a 25; marca: hasta 40 caracteres alfanuméricos, espacios"
              + " o guiones.");
    return vehicles.search(brand == null ? "" : brand.strip(), page, size);
  }
}
