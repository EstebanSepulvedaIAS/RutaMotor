package com.rutamotor.inventory.domain.exception;

public class ReferenceConflict extends RuntimeException {
  public ReferenceConflict() {
    super(
        "La referencia ya fue utilizada con otra unidad o alias. El resultado original se"
            + " conserva.");
  }
}
