package com.rutamotor.inventory.infrastructure.configuration;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class SeedConfiguration {
  @Bean
  ApplicationRunner seed(
      JdbcTemplate jdbc,
      PlatformTransactionManager manager,
      @Value("${rutamotor.seed-size}") int size) {
    return args -> {
      if (size < 6 || size > 100000)
        throw new IllegalArgumentException("Seed size must be 6..100000");
      new TransactionTemplate(manager)
          .executeWithoutResult(
              tx -> {
                String[] brands = {"Toyota", "Mazda", "Renault"};
                String[] models = {"Corolla", "CX-30", "Duster"};
                for (int i = 1; i <= size; i++) {
                  var id = new UUID(0, i);
                  int n = (i - 1) / 2 % 3;
                  jdbc.update(
                      "INSERT INTO vehicles(id,vin,brand,model,price,status,version) SELECT"
                          + " ?,?,?,?,?,?,0 WHERE NOT EXISTS (SELECT 1 FROM vehicles WHERE id=?)",
                      id,
                      String.format("RM%015d", i),
                      brands[n],
                      models[n],
                      75000000L + (i % 20) * 1500000L,
                      i == 6 ? "RESERVED" : "AVAILABLE",
                      id);
                }
              });
    };
  }
}
