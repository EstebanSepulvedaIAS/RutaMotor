package com.rutamotor.inventory.infrastructure.configuration;

import com.rutamotor.inventory.application.service.*;
import com.rutamotor.inventory.domain.port.in.*;
import com.rutamotor.inventory.domain.port.out.*;
import java.time.Clock;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class UseCaseConfiguration {
  @Bean
  SearchCatalogQuery catalog(VehicleRepositoryPort v, PlatformTransactionManager manager) {
    var tx = new TransactionTemplate(manager);
    tx.setReadOnly(true);
    var service = new SearchCatalogService(v);
    return (brand, page, size) -> tx.execute(s -> service.search(brand, page, size));
  }

  @Bean
  ReserveVehicleCommand reservations(
      VehicleRepositoryPort v, ReservationRepositoryPort r, PlatformTransactionManager manager) {
    return new TransactionalReservation(
        new ReserveVehicleService(v, r, Clock.systemUTC()), manager);
  }
}
