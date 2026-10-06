package com.rutamotor.inventory.infrastructure.adapter.out.persistence;

import com.rutamotor.inventory.domain.model.*;
import com.rutamotor.inventory.domain.port.out.VehicleRepositoryPort;
import jakarta.persistence.*;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaVehicleAdapter implements VehicleRepositoryPort {
  @PersistenceContext private EntityManager em;

  public Optional<Vehicle> find(VehicleId id) {
    return Optional.ofNullable(em.find(VehicleJpaEntity.class, id.value()))
        .map(PersistenceMapper::domain);
  }

  public CatalogPage search(String brand, int page, int size) {
    String where = brand.isEmpty() ? "" : " where v.brand=:brand";
    var query =
        em.createQuery(
            "select v from VehicleJpaEntity v" + where + " order by v.id", VehicleJpaEntity.class);
    var count = em.createQuery("select count(v) from VehicleJpaEntity v" + where, Long.class);
    if (!brand.isEmpty()) {
      query.setParameter("brand", brand);
      count.setParameter("brand", brand);
    }
    return new CatalogPage(
        query.setFirstResult(page * size).setMaxResults(size).getResultList().stream()
            .map(PersistenceMapper::domain)
            .toList(),
        page,
        size,
        count.getSingleResult());
  }

  public void save(Vehicle v) {
    // Reuses the managed entity read in this transaction: preserves its original @Version.
    var e = em.find(VehicleJpaEntity.class, v.id().value());
    if (e == null) throw new IllegalStateException("Missing managed vehicle");
    PersistenceMapper.copy(v, e);
    em.flush();
  }
}
