# Carga UY — App Android

Componente móvil de carga.uy para choferes. Kotlin + Jetpack Compose, Room para trabajar sin conexión
y WorkManager para enviar los eventos al componente central cuando hay red.

## Requisitos

- Android Studio reciente (compileSdk 37, AGP 9.4.0, Gradle 9.6, Kotlin 2.4.10, KSP 2.3.12)
- JDK 25
- El backend levantado (ver el README principal)

## Configuración

El celular o el emulador no llegan a la PC por `localhost`: hay que usar la IP de la máquina
(`ipconfig`, "Dirección IPv4").

1. En `carga-uy/.env` (login simulado) y reiniciar el backend:

       GUBUY_MOCK=true
       GUBUY_REDIRECT_URI=http://<IP>:8080/carga-uy/api/auth/callback

   Con esto, el login del frontend web funciona solo si se abre por la IP (`http://<IP>:5173`).

2. En `mobile/local.properties` (no se commitea), la URL inicial de la app:

       cargauy.url=http://<IP>:8080/carga-uy/api/

   En debug se puede cambiar después en Configuración, sin recompilar.

## Ejecutar

Abrir `mobile/` en Android Studio, esperar el sync de Gradle y correr `app`.

- "Ingresar con gub.uy" entra como Carlos (55555555), el chofer sembrado. En debug, el campo
  "Cédula (login simulado)" permite entrar con otro usuario.
- El central rechaza eventos con fecha anterior a la de la Guía: para probarlos hace falta una Guía
  con fecha de hoy y su viaje asignado a Carlos.
- Si tenían instalada una versión anterior de la app, desinstalarla una vez.

## Tests

El CI no compila la app: correr los tests antes de subir cambios.

    .\gradlew.bat assembleDebug testDebugUnitTest

La base local (Room) está en la versión 1. Cambiar una tabla o columna obliga a subir la versión y
agregar una migración; el esquema de cada versión queda en `app/schemas`.
