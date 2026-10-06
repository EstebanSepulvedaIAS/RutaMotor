package com.rutamotor.inventory.infrastructure.configuration;

import com.rutamotor.inventory.domain.model.*;
import com.rutamotor.inventory.domain.port.in.ReserveVehicleCommand;
import org.springframework.dao.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

public final class TransactionalReservation implements ReserveVehicleCommand {
  private final ReserveVehicleCommand delegate;
  private final TransactionTemplate tx;

  public TransactionalReservation(
      ReserveVehicleCommand delegate, PlatformTransactionManager manager) {
    this.delegate = delegate;
    tx = new TransactionTemplate(manager);
    tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  public ReservationResult reserve(ReservationRequest request) {
    for (int attempt = 0; attempt < 4; attempt++) {
      try {
        return tx.execute(status -> delegate.reserve(request));
      } catch (OptimisticLockingFailureException | DataIntegrityViolationException collision) {
        // Rollback is complete before retrying. Re-read winner/result in a NEW persistence context.
        if (attempt == 3) throw new RetryLaterException();
      }
    }
    throw new RetryLaterException();
  }

  public static class RetryLaterException extends RuntimeException {}
}
