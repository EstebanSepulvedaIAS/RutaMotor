# RutaMotor — catálogo y reserva de vehículos

Aplicación local de una pantalla: Angular consulta unidades físicas y envía reservas a Java/Spring Boot. Cada referencia conserva su primer resultado definitivo, aceptado o rechazado. La reserva y el resultado se confirman en una sola transacción. H2 guarda los datos en disco.

## Organización

```text
rutamotor/
├── backend/       Java 21 + Spring Boot, Gradle Wrapper y pruebas
├── frontend/      Angular + TypeScript, pruebas unitarias y navegador
├── database/      Esquema SQL, documentación y archivos locales de H2
├── scripts/       Verificación HTTP de 10.000 unidades y reinicio
```

Las tres aplicaciones/capas se entregan en carpetas separadas dentro de una solución. H2 es un motor embebido: **no necesita arrancar un tercer servidor**. El backend incorpora `database/sql/schema.sql` durante la construcción; conserve ambas carpetas hermanas. No se requiere Docker, cuentas de nube ni servicios externos durante la ejecución.

## 1. Requisitos

| Herramienta | Versión de la entrega |
|---|---|
| Java JDK | 21; verificado con Corretto 21.0.12.1 |
| Spring Boot | 3.4.13 |
| Gradle | 8.14.2, Wrapper incluido |
| Hibernate / H2 | 6.6.39.Final / 2.3.232, gestionados por Spring Boot |
| Node.js / npm | 22.16.0 / 10.9.2 verificados; use Node 22.x |
| Angular framework y CLI | 19.2.15 |
| TypeScript / RxJS | 5.8.3 / 7.8.2 |
| Pruebas frontend | Jest 29.7.0, jest-preset-angular 14.6.1, Playwright 1.55.1 |
| Python, opcional | 3.10+ para verificar rendimiento y reinicios |


Compruebe `java -version`, `javac -version`, `node --version` y `npm --version`. Configure `JAVA_HOME` al **JDK 21**, no a un JRE antiguo. En PowerShell, si hace falta:

```powershell
$env:JAVA_HOME = 'C:\ruta\a\jdk-21'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

## 2. Ejecutar backend y base de datos

Abra una terminal en `rutamotor/backend`:

```powershell
.\gradlew.bat test bootJar
.\gradlew.bat bootRun
```

En Linux/macOS: `chmod +x gradlew`, `./gradlew test bootJar`, `./gradlew bootRun`.

El backend escucha en [http://localhost:8080/api/v1/vehicles](http://localhost:8080/api/v1/vehicles). Crea el esquema y agrega seis unidades ficticias: Toyota, Mazda y Renault, dos unidades de cada modelo y una ya separada. Al reiniciar solo agrega IDs faltantes: **no borra ni sobrescribe reservas**.

Los datos quedan en `database/data/rutamotor.mv.db`. Para ejecutar el JAR directamente, mantenga la terminal en `backend`:

```powershell
java -jar build/libs/rutamotor-backend-1.0.0.jar
```

**Caso particular observado en este Windows:** si Gradle falla con `Unable to establish loopback connection` y la causa menciona `UnixDomainSockets.connect0`, se verificó esta alternativa de diagnóstico para el JDK de este equipo:

```powershell
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=C:\rutamotor-unix-sockets-disabled'
.\gradlew.bat test bootRun
```

## 3. Ejecutar frontend

Abra una segunda terminal en `rutamotor/frontend`:

```powershell
npm ci
npm start
```

Abra [http://localhost:4200](http://localhost:4200). El proxy de Angular redirige `/api` al backend. Seleccione una marca, pulse **Separar unidad**, escriba un alias ficticio y confirme. La pantalla mostrará referencia, resultado y precio aplicado cuando se acepte.

Si la respuesta se pierde, aparece **Resultado sin confirmar**. Use **Reintentar misma solicitud**: conserva referencia, vehículo y alias; no inventa un resultado. La intención pendiente se guarda en `sessionStorage` para sobrevivir a una recarga de la misma pestaña. Con un resultado definitivo, seleccionar otra vez una unidad inicia una intención nueva con otro UUID. Una intención pendiente no puede editarse ni cancelarse desde el modal hasta recuperar su resultado.

Para detener cada proceso, presione `Ctrl+C` en su terminal. Reiniciar con la misma ruta de base de datos conserva las unidades y resultados.

## 4. Generar 10.000 unidades

Detenga el backend y vuelva a iniciarlo desde `backend`:

```powershell
.\gradlew.bat bootRun --args="--rutamotor.seed-size=10000"
```

O `java -jar build/libs/rutamotor-backend-1.0.0.jar --rutamotor.seed-size=10000`.

Se insertan los IDs determinísticos faltantes hasta llegar a 10.000. Volver al valor 6 **no elimina** las otras unidades. La consulta hace paginación en SQL, con máximo 25 filas por respuesta y orden ascendente por ID. Para una demostración nueva sin tocar datos existentes, use otra ruta:

```powershell
java -jar build/libs/rutamotor-backend-1.0.0.jar "--spring.datasource.url=jdbc:h2:file:../database/data/otra-demo;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0"
```

No ejecute dos backend sobre el mismo archivo H2. Si necesita copiarlo como respaldo, detenga antes el backend.

## 5. Pruebas y comprobaciones

Backend, desde `backend`:

```powershell
.\gradlew.bat test
```

Reporte: `backend/build/reports/tests/test/index.html`. Cubre dominio, casos de uso, REST, bloqueo optimista real, colisiones, referencias concurrentes y reversión de cambios.

Frontend, desde `frontend`:

```powershell
npm test
npm run build
```

Flujo real en navegador, con backend en 8080 y Angular en 4200:

```powershell
npx playwright install chromium
npm run e2e
```

Estas pruebas realizan reservas ficticias reales. Use una base de demostración con unidades disponibles; para repetir sin agotar las dos Toyota de la muestra inicial, use el catálogo de 10.000 o una ruta de base nueva. Verifican reserva desde Angular, estado actualizado, recarga de la página y recuperación de una respuesta perdida. No sustituyen el backend por un mock.

Verificación aislada de rendimiento y persistencia, desde `rutamotor`:

```powershell
python scripts/verify_runtime.py
```