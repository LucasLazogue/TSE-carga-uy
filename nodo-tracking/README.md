## Nodo periférico de tracking

Simula el Sistema de Tracking: cada vehículo de la flota avanza sobre su ruta y, cada intervalo, el
nodo publica su posición en la cola `queue_tracking` del componente central (CU-10). Publica directo
en el broker del central (Artemis, en su propio contenedor), con TLS y con un usuario que solo puede
publicar en esa cola.

- Cada evento lleva un `idEvento` propio, la matrícula, la posición y la hora en que se generó.
- Al llegar a destino, el vehículo emprende la vuelta, así que el flujo es continuo.
- Si el broker no responde, los eventos quedan pendientes y se publican cuando vuelve. Si el que no
  responde es WildFly, el nodo publica igual y las posiciones esperan en la cola.
- La flota por defecto (`src/main/resources/rutas-prueba.json`) coincide con los datos de prueba
  del central.

### Cómo levantarlo

Junto al central, desde `carga-uy`:

    docker compose up --build -d

El nodo tiene que estar registrado y habilitado en el central con el identificador de `NODO_ID`
(`tracking-1` en el compose); si no, la ingesta descarta sus eventos. Hay que hacerlo una vez, y de
nuevo después de cada `docker compose down -v`. Desde el backoffice, en
http://localhost:8080/carga-uy/nodos.xhtml (identificador `tracking-1`, tipo `TRACKING`, y
habilitarlo), o con la API:

    curl -X POST -H 'Content-Type: application/json' \
      -d '{"identificador":"tracking-1","tipo":"TRACKING","puntoAcceso":"http://localhost:8081"}' \
      http://localhost:8080/carga-uy/api/nodos
    curl -X POST http://localhost:8080/carga-uy/api/nodos/1/habilitar

Fuera de Docker (PowerShell, desde `nodo-tracking`, con el central levantado). Por defecto se conecta
a `localhost:61617`, el puerto con TLS del broker, y confía en el certificado de desarrollo de
`tls/confianza.p12`:

    mvn -q -DskipTests package
    $env:NODO_ID="tracking-1"; $env:NODO_JMS_USUARIO="tracking-1"; $env:NODO_JMS_CLAVE="tracking123"
    java -cp "target/nodo-tracking.jar;target/lib/*" tse.cargauy.nodotracking.NodoTracking

### Configuración

| Variable | Por defecto | Para qué |
| --- | --- | --- |
| `NODO_ID` | obligatoria | Identificador del nodo registrado en el central |
| `NODO_JMS_USUARIO` / `NODO_JMS_CLAVE` | obligatorias | Usuario del nodo en el broker (`tracking-1` / `tracking123` en desarrollo) |
| `NODO_JMS_URL` | `tcp://localhost:61617?sslEnabled=true&trustStorePath=tls/confianza.p12&trustStorePassword=cargauy-dev` | Dirección del broker, con TLS |
| `NODO_PUERTO` | `8081` | Puerto de `/health` y `/metricas` |
| `NODO_MAX_LOTE` | `500` | Máximo de eventos por publicación |
| `NODO_RUTAS` | `rutas-prueba.json` | Archivo JSON con la flota (admite `desvioKm` por vehículo) |
| `NODO_VEHICULOS_EXTRA` | `0` | Vehículos `SIM0001...` con rutas al azar, para la prueba de carga |
| `NODO_INTERVALO_SEG` | `10` | Cada cuánto reporta cada vehículo |
| `NODO_ACELERAR` | `1` | Multiplica la distancia de cada tick; la hora sigue siendo la real |
| `NODO_DUPLICADOS` | `0` | Fracción de eventos que se publican dos veces |
| `NODO_DESORDENAR` | `false` | Mezcla cada lote antes de publicarlo |
| `NODO_SIN_COBERTURA` | vacío | `desde,durante` en minutos: acumula y publica todo junto al volver |
| `NODO_LOG_METRICAS_SEG` | `60` | Cada cuánto escribe las métricas en el log (`0` = nunca) |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4318` | Plataforma de Observabilidad (OTLP por HTTP) |
| `OTEL_TRACES_SAMPLER` / `OTEL_TRACES_SAMPLER_ARG` | todas las trazas | Proporción de trazas que se guardan; tiene que ser la misma que en el central |
| `OTEL_SDK_DISABLED` | `false` | `true` apaga trazas, métricas y logs por OTLP |

Por ejemplo, desde `carga-uy`:

    NODO_ACELERAR=60 NODO_DUPLICADOS=0.3 NODO_DESORDENAR=true docker compose up -d nodo-tracking

### Cómo ver lo que hace

- `GET http://localhost:8081/health`: `200 {"estado":"UP"}` si está conectado al broker; si no,
  `503 {"estado":"DOWN"}`.
- `GET http://localhost:8081/metricas`: generados, duplicados, publicados, pendientes y reintentos.
  Las mismas métricas van por OTLP a la Plataforma de Observabilidad, junto con una traza por cada
  lote publicado (el contexto viaja en la propiedad `traceparent` de cada mensaje) y los logs.
  Las mismas métricas quedan en el log cada `NODO_LOG_METRICAS_SEG` segundos
  (`docker compose logs nodo-tracking`).
- Ruta de un vehículo en el central: http://localhost:8080/carga-uy/api/posiciones?matricula=STA1234
- Qué hizo la ingesta con cada evento: `docker compose logs carga-uy | grep ingesta`

Al detenerlo (`docker compose stop nodo-tracking`) publica lo pendiente e imprime las métricas
finales. `generados` tiene que coincidir con los documentos nuevos de la colección `posiciones` de
MongoDB (desde `carga-uy`):

    docker compose exec mongo mongosh -u cargauy -p cargauy --quiet --eval 'db.getSiblingDB("cargauy").posiciones.countDocuments()'

### Tests

    mvn verify

`mvn test -Dtest=PruebaManualPublicador` publica dos eventos en el central real (necesita el central
levantado y `tracking-1` habilitado).
