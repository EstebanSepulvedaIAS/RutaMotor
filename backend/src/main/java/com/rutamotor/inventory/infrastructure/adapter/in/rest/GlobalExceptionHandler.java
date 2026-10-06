package com.rutamotor.inventory.infrastructure.adapter.in.rest;

import com.rutamotor.inventory.domain.exception.ReferenceConflict;
import com.rutamotor.inventory.infrastructure.configuration.TransactionalReservation.RetryLaterException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler({
    IllegalArgumentException.class,
    MethodArgumentNotValidException.class,
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class
  })
  ResponseEntity<ProblemDetail> invalid(Exception e, HttpServletRequest request) {
    return error(
        400,
        "Entrada inválida",
        "Revise UUID, alias (2–40 caracteres ASCII, sin espacios extremos), página y tamaño. No"
            + " envíe campos adicionales.",
        request);
  }

  @ExceptionHandler(ReferenceConflict.class)
  ResponseEntity<ProblemDetail> conflict(ReferenceConflict e, HttpServletRequest request) {
    return error(409, "Referencia en conflicto", e.getMessage(), request);
  }

  @ExceptionHandler({RetryLaterException.class, DataAccessException.class})
  ResponseEntity<ProblemDetail> unavailable(Exception e, HttpServletRequest request) {
    return error(
        503,
        "Resultado sin confirmar",
        "Reintente con la misma referencia y los mismos datos.",
        request);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> unexpected(Exception e, HttpServletRequest request) {
    return error(
        500,
        "Resultado sin confirmar",
        "No se pudo confirmar el resultado. Reintente con la misma referencia.",
        request);
  }

  private ResponseEntity<ProblemDetail> error(
      int status, String title, String detail, HttpServletRequest request) {
    var p = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), detail);
    p.setTitle(title);
    p.setInstance(URI.create(request.getRequestURI()));
    return ResponseEntity.status(status).body(p);
  }
}
