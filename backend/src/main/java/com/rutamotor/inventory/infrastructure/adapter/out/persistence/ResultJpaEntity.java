package com.rutamotor.inventory.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservation_results")
public class ResultJpaEntity {
  @Id UUID reference;

  @Column(nullable = false)
  UUID vehicleId;

  @Column(nullable = false, length = 40)
  String buyerAlias;

  @Column(nullable = false)
  Instant processedAt;

  @Column(nullable = false, length = 16)
  String outcome;

  @Column(length = 40)
  String reason;

  @Column(precision = 15, scale = 2)
  BigDecimal appliedPrice;

  protected ResultJpaEntity() {}
}
