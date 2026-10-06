# Frontend RutaMotor

Angular 19.2.15 standalone, TypeScript 5.8.3, Node 22.x. Desde esta carpeta:

```powershell
npm ci
npm start
```

Abra [http://localhost:4200](http://localhost:4200). Requiere backend en 8080. `proxy.conf.json` configura la comunicación local, sin URLs del backend incrustadas en componentes.

```powershell
npm test
npm run build
npx playwright install chromium
npm run e2e
```

El último comando requiere ambos servidores y realiza reservas reales sobre datos ficticios. Las pruebas no necesitan cuentas externas.

`core` guarda la intención pendiente; `features/inventory/models` define contratos; `services` encapsula HTTP; `pages/catalog` orquesta estado con Signals y RxJS; `components` contiene tarjeta y modal presentacionales con OnPush. Las suscripciones usan `takeUntilDestroyed`; `switchMap` evita que respuestas viejas sobrescriban un filtro nuevo.

El diálogo nativo proporciona foco y modalidad. El formulario valida antes de emitir el alias. El contenedor decide cuándo enviar, reintentar o mostrar resultados; un error de transporte no se presenta como rechazo comercial. Un UUID nuevo identifica cada intención nueva. Mientras se desconoce el resultado, el reintento usa exactamente el cuerpo original, también tras recargar la pestaña si `sessionStorage` está disponible.

Límite: cerrar la pestaña puede perder su intención local; no existe un historial de consultas en esta demostración. Conserve la referencia para diagnóstico. Si el almacenamiento del navegador está deshabilitado, el reintento se conserva solo mientras la página esté abierta.

El catálogo muestra tanto disponibles como separados para observar la transición. Las tres marcas del selector corresponden a los datos reproducibles de esta demo. Los estilos son CSS global simple con componentes visuales propios, sin servicios externos ni fuentes descargadas.
