package com.rutamotor.inventory.infrastructure.adapter.out.persistence;

import com.rutamotor.inventory.domain.model.*;
import com.rutamotor.inventory.domain.port.out.ReservationRepositoryPort;
import jakarta.persistence.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class JpaResultAdapter implements ReservationRepositoryPort {
  @PersistenceContext private EntityManager em;

  public Optional<ReservationResult> find(UUID ref) {
    return Optional.ofNullable(em.find(ResultJpaEntity.class, ref)).map(PersistenceMapper::domain);
  }

  public void save(ReservationResult r) {
    em.persist(PersistenceMapper.entity(r));
    em.flush();
  }
}
