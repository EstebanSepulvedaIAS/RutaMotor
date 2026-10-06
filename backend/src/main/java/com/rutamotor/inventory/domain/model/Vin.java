package com.rutamotor.inventory.domain.model;

public record Vin(String value) {
  public Vin {
    if (value == null || !value.matches("[A-HJ-NPR-Z0-9]{17}"))
      throw new IllegalArgumentException("VIN inválido");
  }
}
