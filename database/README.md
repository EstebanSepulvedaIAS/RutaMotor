# Base de datos RutaMotor

H2 2.3.232 embebida y persistente en archivo. No hay proceso ni instalación SQL separados: el backend arranca el motor, aplica `sql/schema.sql` de forma no destructiva y agrega únicamente datos faltantes.

## Arranque y datos

1. Ejecute el backend desde `../backend` siguiendo el README principal.
2. Se crea `data/rutamotor.mv.db` con seis unidades; reiniciar conserva su contenido.
3. Para 10.000 unidades, reinicie con `--rutamotor.seed-size=10000`.
4. Para otro conjunto nuevo, use una ruta distinta en `--spring.datasource.url`, conservando `WRITE_DELAY=0`.

## Esquema

- `vehicles`: UUID de unidad, VIN único, marca, modelo, precio COP, estado y versión de bloqueo optimista. No hay un contador de existencias. Índice `(brand,id)` para filtro y recorrido.
- `reservation_results`: referencia UUID como clave primaria, unidad solicitada, alias, fecha UTC, outcome, razón y precio aceptado. La clave impide duplicar la referencia. No hay FK a vehículo porque el rechazo de una unidad inexistente también debe persistirse. Restricciones CHECK impiden resultados aceptados sin precio y rechazados sin razón.

El resultado aceptado y la transición de la unidad comparten transacción. Las fechas se normalizan a microsegundos antes de responder para conservar exactamente el valor almacenado. `WRITE_DELAY=0` elimina el retraso de escritura del log entre commit y persistencia; se comprobó también tras terminar abruptamente el proceso.

La generación determinística está en `SeedConfiguration` del backend: UUID con secuencia numérica, VIN ficticio de 17 caracteres, marcas/modelos cíclicos, precios reproducibles. Los primeros dos registros representan dos Corolla diferentes; el sexto ya está reservado como estado inicial histórico de demostración. Esa fila no simula una solicitud previamente procesada por la aplicación.

La consola H2 no está habilitada. No es necesaria para la evaluación y la base se comprueba con los endpoints y pruebas. El archivo se abre en una sola JVM; no admite dos instancias backend simultáneas. Para respaldo, detenga el servicio y copie `data`. No comparta los archivos de `data` en Git.

No se usa `ddl-auto=create` ni recreación de tablas: el esquema es explícito y Hibernate solo lo valida. Para una evolución posterior del esquema se requerirían migraciones versionadas; no modifique columnas de una base existente sin migración y respaldo.
