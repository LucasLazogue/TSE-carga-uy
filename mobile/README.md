# Carga UY — App Android

[Volver al proyecto](../README.md)

Componente móvil de carga.uy para choferes. Kotlin + Jetpack Compose, Room para trabajar sin conexión
y WorkManager para enviar los eventos al componente central cuando hay red.

## Requisitos

- Android Studio con el SDK y las herramientas definidos en [app/build.gradle.kts](app/build.gradle.kts)
  y el [catálogo de versiones](gradle/libs.versions.toml).
- JDK 25
- El [entorno local levantado](../README.md#ejecución-local).

## Configuración

Hay dos formas: con el login simulado o con gub.uy de testing. En las dos, si se pone
`CARGAUY_JWT_SECRET` en `carga-uy/.env` tiene que tener al menos 32 bytes; si no, todo login vuelve con
`?error=4008` (vacío usa el de desarrollo del compose).

### Login simulado

El celular o el emulador no llegan a la PC por `localhost`: hay que usar la IP de la máquina
(`ipconfig`, "Dirección IPv4").

1. En `carga-uy/.env`, configurar el login simulado:

       GUBUY_MOCK=true
       GUBUY_REDIRECT_URI=http://<IP>:8080/carga-uy/api/auth/callback

   Si también se usa el frontend web, abrirlo por esa IP y configurar
   `CARGAUY_FRONTEND_URL=http://<IP>:5173` en el mismo `.env`. Para exponer Vite a la red local,
   ejecutar `npm run dev -- --host 0.0.0.0` desde `frontend`. Ver las
   [URLs del login](../docs/seguridad.md#urls-del-login).

   Desde `carga-uy`, ejecutar `docker compose up -d carga-uy` para aplicar las nuevas variables.

2. En `mobile/local.properties` (no se commitea), la URL inicial de la app:

       cargauy.url=http://<IP>:8080/carga-uy/api/

   En debug se puede cambiar después en Configuración, sin recompilar.

3. Opcional, en el mismo `local.properties`, la Plataforma de Observabilidad (OTLP por HTTP), para ver en
   Grafana la traza que empieza en el celular:

       cargauy.observabilidad=http://<IP>:4318

   Cada pedido al central abre un span y le pasa la traza en el header `traceparent`. Sin esta URL los spans
   no se exportan, pero el header se manda igual. Ver la [documentación de observabilidad](../docs/observabilidad.md).

   Con la plataforma de OpenShift, la URL es la de la Route `observabilidad-otlp-movil` y hace falta su token
   (`OTLP_MOVIL_TOKEN` del Secret `observabilidad-secretos`, ver el
   [despliegue](../docs/observabilidad.md#despliegue-en-openshift)):

       cargauy.observabilidad=https://<HOST_DE_LA_ROUTE>
       cargauy.observabilidad.token=<TOKEN>

   El token queda dentro del APK, así que se asume que se puede extraer: solo permite mandar trazas, y el
   router limita los pedidos por IP.

### Login con gub.uy de testing

gub.uy solo vuelve a la redirect URI registrada para el cliente, `http://localhost:8080`, y en el
celular `localhost` es el propio celular. Por eso se redirige el puerto 8080 del dispositivo a la PC con
`adb reverse` y la app usa `localhost` en vez de la IP.

1. Configurar el `.env` del backend como dice
   [gub.uy de testing](../docs/seguridad.md#gubuy-de-testing).

2. En `mobile/local.properties`:

       cargauy.url=http://localhost:8080/carga-uy/api/

   Tiene que ser `localhost` y no la IP: el estado del login viaja en la cookie `cargauy_login`, que el
   navegador solo manda al mismo host. Si el login empieza por la IP y vuelve por `localhost`, el central
   no reconoce que es de la app y redirige al frontend (`http://localhost:5173/?error=...`). Si en debug
   se guardó una URL en Configuración, esa manda sobre la de `local.properties`: cambiarla ahí también.

3. Con el emulador abierto (o el celular conectado por USB), cada vez que arranca:

       adb reverse tcp:8080 tcp:8080

   Sin esto la app no llega al central. Si `adb` no está en el PATH, está en la carpeta `platform-tools`
   del SDK (la ruta del SDK se ve en Android Studio, Settings > Languages & Frameworks > Android SDK).

El campo "Cédula (login simulado)" se sigue viendo en debug, pero con `GUBUY_MOCK=false` el central lo
ignora: "Ingresar con gub.uy" abre el login real (la cuenta de testing, ver la
[documentación de seguridad](../docs/seguridad.md#gubuy-de-testing)).

## Ejecutar

Abrir `mobile/` en Android Studio, esperar el sync de Gradle y correr `app`.

- Con el login simulado, "Ingresar con gub.uy" entra como Carlos (55555555), el chofer sembrado.
  En debug, el campo "Cédula (login simulado)" permite entrar con otro usuario.
- El central rechaza eventos con fecha anterior a la de la Guía. Para probar el envío, crear una Guía
  con fecha de hoy y asignarle el viaje a Carlos.

El canje de códigos, la renovación de tokens y DPoP están descritos en la
[documentación de seguridad](../docs/seguridad.md#sesiones-web-y-móviles).

## Tests

El [CI](../.github/workflows/ci.yml) compila, ejecuta los tests unitarios y corre Android Lint.
Para hacer lo mismo localmente, desde `mobile` en PowerShell:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

La base local (Room) está en la versión 2 y tiene migración de 1 a 2 en
[CargaUYDatabase](app/src/main/java/uy/tse/cargauy/data/local/CargaUYDatabase.kt).
Cambiar una tabla o columna requiere subir la versión y agregar una migración;
los esquemas se conservan en [app/schemas](app/schemas/).
