package com.rutamotor.inventory.infrastructure.adapter.in.rest;

import com.rutamotor.inventory.domain.model.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class RestDtos {
  private RestDtos() {}

  public record ReserveRequest(
      @NotNull UUID reference,
      @NotNull UUID vehicleId,
      @NotNull @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9 _-]{1,39}") String buyerAlias) {
    public ReservationRequest domain() {
      return new ReservationRequest(reference, new VehicleId(vehicleId), new Buyer(buyerAlias));
    }
  }

  public record VehicleResponse(
      UUID id,
      String vin,
      String brand,
      String model,
      BigDecimal price,
      String currency,
      String status) {
    public static VehicleResponse from(Vehicle v) {
      return new VehicleResponse(
          v.id().value(),
          v.vin().value(),
          v.brand(),
          v.model(),
          v.price().amount(),
          v.price().currency(),
          v.status().name());
    }
  }

  public record CatalogResponse(List<VehicleResponse> items, int page, int size, long total) {
    public static CatalogResponse from(CatalogPage p) {
      return new CatalogResponse(
          p.items().stream().map(VehicleResponse::from).toList(), p.page(), p.size(), p.total());
    }
  }

  public record ResultResponse(
      UUID reference,
      UUID vehicleId,
      String buyerAlias,
      Instant processedAt,
      String outcome,
      String reason,
      BigDecimal appliedPrice,
      String currency) {
    public static ResultResponse from(ReservationResult r) {
      return new ResultResponse(
          r.request().reference(),
          r.request().vehicleId().value(),
          r.request().buyer().alias(),
          r.processedAt(),
          r.outcome().name(),
          r.reason() == null ? null : r.reason().name(),
          r.appliedPrice() == null ? null : r.appliedPrice().amount(),
          "COP");
    }
  }
}
