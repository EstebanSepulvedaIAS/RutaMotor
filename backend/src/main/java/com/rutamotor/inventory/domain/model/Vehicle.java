package com.rutamotor.inventory.domain.model;

import java.util.Objects;

public final class Vehicle {
  private final VehicleId id;
  private final Vin vin;
  private final String brand, model;
  private final Money price;
  private VehicleStatus status;

  public Vehicle(
      VehicleId id, Vin vin, String brand, String model, Money price, VehicleStatus status) {
    this.id = Objects.requireNonNull(id);
    this.vin = Objects.requireNonNull(vin);
    if (brand == null || brand.isBlank() || model == null || model.isBlank())
      throw new IllegalArgumentException("Marca/modelo requeridos");
    this.brand = brand;
    this.model = model;
    this.price = Objects.requireNonNull(price);
    this.status = Objects.requireNonNull(status);
  }

  public boolean reserveFor(Buyer buyer) {
    Objects.requireNonNull(buyer);
    if (status != VehicleStatus.AVAILABLE) return false;
    status = VehicleStatus.RESERVED;
    return true;
  }

  public VehicleId id() {
    return id;
  }

  public Vin vin() {
    return vin;
  }

  public String brand() {
    return brand;
  }

  public String model() {
    return model;
  }

  public Money price() {
    return price;
  }

  public VehicleStatus status() {
    return status;
  }
}
