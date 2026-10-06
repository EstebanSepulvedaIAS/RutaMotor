package com.rutamotor.inventory.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
public class VehicleJpaEntity {
  @Id UUID id;

  @Column(nullable = false, unique = true, length = 17)
  String vin;

  @Column(nullable = false, length = 40)
  String brand;

  @Column(nullable = false, length = 60)
  String model;

  @Column(nullable = false, precision = 15, scale = 2)
  BigDecimal price;

  @Column(nullable = false, length = 16)
  String status;

  @Version long version;

  protected VehicleJpaEntity() {}
}
