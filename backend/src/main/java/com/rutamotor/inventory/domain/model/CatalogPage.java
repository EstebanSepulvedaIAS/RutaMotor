package com.rutamotor.inventory.domain.model;

import java.util.List;

public record CatalogPage(List<Vehicle> items, int page, int size, long total) {
  public CatalogPage {
    items = List.copyOf(items);
  }
}
