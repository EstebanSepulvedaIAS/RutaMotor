# Backend RutaMotor

Java 21, Spring Boot 3.4.13, Gradle Wrapper 8.14.2. Ejecute desde esta carpeta:

```powershell
.\gradlew.bat test bootJar
.\gradlew.bat bootRun
```

Linux/macOS: `./gradlew test bootJar` y `./gradlew bootRun`.

Conserve `../database`: Gradle incorpora su esquema SQL como recurso. La base predeterminada persiste en `../database/data/rutamotor`; la carpeta de trabajo del proceso debe ser `backend`. Para cambiarla configure `DB_URL` o `--spring.datasource.url=...`, conservando `WRITE_DELAY=0` para escritura inmediata. `SEED_SIZE` o `--rutamotor.seed-size=10000` amplía el catálogo; solo admite 6 a 100.000 unidades.

## Arquitectura

`domain` contiene entidades, valores y puertos en Java puro. `application/service` implementa consulta y reserva. `infrastructure` contiene DTOs REST, manejador de errores, mapeadores, entidades JPA, adaptadores y configuración transaccional. No hay anotaciones Spring/JPA en dominio ni aplicación.

El decorador `TransactionalReservation` abre una transacción nueva para cada intento. Primero busca la referencia; si existe, valida que unidad y alias coincidan y devuelve el resultado original. Si no existe, consulta la unidad, aplica su invariante y persiste el resultado. `@Version` evita actualizaciones desde lecturas obsoletas. Un conflicto de versión o clave primaria revierte toda la transacción; un nuevo intento relee al ganador. Máximo cuatro intentos; un fallo técnico sin resultado definitivo produce 503 y exige reintentar la misma referencia. No se bloquea todo el catálogo con un mutex global.

El adaptador usa `EntityManager` de JPA directamente y excepciones traducidas por `@Repository`; no necesita interfaces adicionales de Spring Data para estas dos consultas. Los objetos JPA no salen al controlador. Los puertos se ubican en `domain/port` siguiendo `02_ENGINEERING_RULES.md`; la discrepancia documental con `application/port` está registrada.

Logs de resultado: referencia UUID y outcome, sin alias. El precio siempre proviene de la unidad persistida. Los campos JSON desconocidos, incluido un precio proporcionado por el cliente, son rechazados.

Contrato: [API](../docs/API.md). Arranque completo y solución del problema local de sockets: [README principal](../README.md).
