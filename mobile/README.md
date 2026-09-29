# Carga UY — App Android

Componente móvil de carga.uy para choferes. Kotlin + Jetpack Compose, navegación con
Navigation Compose, base de datos local con Room y preferencias (tema oscuro) con DataStore.

Por ahora no se conecta al componente central: inicializa la base local (`carga-uy.db`,
tabla `evento_pendiente`) y tiene la pantalla de Configuración.

## Requisitos

- Android Studio reciente (compileSdk 37, AGP 9.4.0, Gradle 9.6, Kotlin 2.4.10, KSP 2.3.12)
- JDK 25

## Ejecutar

Abrir el directorio `mobile/` en Android Studio, esperar el sync de Gradle y correr `app`.