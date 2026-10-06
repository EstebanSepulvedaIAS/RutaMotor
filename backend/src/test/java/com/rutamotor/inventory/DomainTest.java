package com.rutamotor.inventory;

import static org.assertj.core.api.Assertions.*;

import com.rutamotor.inventory.domain.model.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DomainTest {
  @Test
  void onlyOneReservationPerPhysicalUnit() {
    var v =
        new Vehicle(
            new VehicleId(UUID.randomUUID()),
            new Vin("RM000000000000001"),
            "Toyota",
            "Corolla",
            new Money(new BigDecimal("85000000")),
            VehicleStatus.AVAILABLE);
    assertThat(v.reserveFor(new Buyer("Buyer-01"))).isTrue();
    assertThat(v.reserveFor(new Buyer("Buyer-02"))).isFalse();
    assertThat(v.status()).isEqualTo(VehicleStatus.RESERVED);
  }

  @Test
  void validatesValueObjects() {
    assertThatThrownBy(() -> new Buyer(" x")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Buyer("x ")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Vin("short")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Money(BigDecimal.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
