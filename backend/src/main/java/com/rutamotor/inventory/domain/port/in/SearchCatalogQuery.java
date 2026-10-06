package com.rutamotor.inventory.domain.port.in;

import com.rutamotor.inventory.domain.model.*;

public interface SearchCatalogQuery {
  CatalogPage search(String brand, int page, int size);
}
