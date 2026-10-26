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
