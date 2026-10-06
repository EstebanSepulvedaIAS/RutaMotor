package com.rutamotor.inventory.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount) {
  public Money {
    if (amount == null
        || amount.signum() <= 0
        || amount.compareTo(new BigDecimal("9999999999999.99")) > 0)
      throw new IllegalArgumentException("Precio inválido");
    amount = amount.setScale(2, RoundingMode.UNNECESSARY);
  }

  public String currency() {
    return "COP";
  }
}
