package com.rutamotor.inventory.infrastructure.adapter.in.rest;

import com.rutamotor.inventory.domain.model.ReservationResult;
import com.rutamotor.inventory.domain.port.in.*;
import jakarta.validation.Valid;
import java.net.URI;
import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(
    origins = "http://localhost:4200",
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class VehicleRestController {
  private final SearchCatalogQuery catalog;
  private final ReserveVehicleCommand reservations;
  private static final Logger log = LoggerFactory.getLogger(VehicleRestController.class);

  public VehicleRestController(SearchCatalogQuery catalog, ReserveVehicleCommand reservations) {
    this.catalog = catalog;
    this.reservations = reservations;
  }

  @GetMapping("/vehicles")
  public RestDtos.CatalogResponse catalog(
      @RequestParam(defaultValue = "") String brand,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return RestDtos.CatalogResponse.from(catalog.search(brand, page, size));
  }

  @PostMapping("/reservations")
  public ResponseEntity<?> reserve(@Valid @RequestBody RestDtos.ReserveRequest request) {
    var result = reservations.reserve(request.domain());
    var dto = RestDtos.ResultResponse.from(result);
    log.info("reservation reference={} outcome={}", result.request().reference(), result.outcome());
    if (result.outcome() == ReservationResult.Outcome.ACCEPTED) return ResponseEntity.ok(dto);
    var status =
        result.reason() == ReservationResult.Reason.NOT_FOUND
            ? HttpStatus.NOT_FOUND
            : HttpStatus.CONFLICT;
    var problem =
        ProblemDetail.forStatusAndDetail(
            status,
            result.reason() == ReservationResult.Reason.NOT_FOUND
                ? "La unidad solicitada no existe."
                : "La unidad ya no está disponible.");
    problem.setTitle("Solicitud rechazada");
    problem.setType(URI.create("urn:rutamotor:" + result.reason().name().toLowerCase()));
    problem.setInstance(URI.create("/api/v1/reservations"));
    problem.setProperty("result", dto);
    return ResponseEntity.status(status).body(problem);
  }
}
