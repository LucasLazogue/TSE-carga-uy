# Nodo periférico de tracking

[Volver al proyecto](../README.md)

Simula el Sistema de Tracking: cada vehículo de la flota avanza sobre su ruta y, cada intervalo, el
nodo publica su posición en la cola `queue_tracking` del componente central (CU-10). Publica directo
en el broker del central (Artemis, en su propio contenedor), con TLS y con un usuario que solo puede
publicar en esa cola.

- Cada evento lleva un `idEvento` propio, la matrícula, la posición y la hora en que se generó.
- Al llegar a destino, el vehículo emprende la vuelta, así que el flujo es continuo.
- Si el broker no responde, los eventos quedan pendientes y se publican cuando vuelve. Si el que no
  responde es WildFly, el nodo publica igual y las posiciones esperan en la cola.
- La flota por defecto (`src/main/resources/rutas-prueba.json`) coincide con los datos de prueba
  del central. El compose levanta dos nodos, como dos proveedores de seguimiento, y la reparte:
  `tracking-1` con `flotas/flota-sur.json` (STA1234, SBB5678) y `tracking-2` con
  `flotas/flota-norte.json` (SCC9012, SDD3456). Cada uno tiene su usuario en el broker.

## Cómo levantarlo

Preparar el [entorno local](../README.md#ejecución-local); el Compose ya incluye este nodo.

Cada nodo tiene que estar registrado y habilitado en el central con el identificador de `NODO_ID`
(`tracking-1` y `tracking-2` en el compose); si no, la ingesta descarta sus eventos
(`NODO_DESCONOCIDO` o `NODO_NO_HABILITADO`) y los del otro nodo siguen entrando. Hay que hacerlo una vez, y de
nuevo después de cada `docker compose down -v`. Desde el backoffice, en
http://localhost:8080/carga-uy/admin/nodos.xhtml, ingresar con el administrador LDAP configurado
en el README principal, registrar los identificadores `tracking-1` y `tracking-2` con tipo `TRACKING` y habilitarlos.
La API `/api/nodos` también requiere una sesión con rol `ADMINISTRADOR`.

El contrato de posiciones, la persistencia y las reentregas están documentados en
[mensajería](../docs/mensajeria.md). Los permisos del broker y la conexión TLS se describen en
[seguridad](../docs/seguridad.md#seguridad-de-los-nodos).

Fuera de Docker (PowerShell, desde `nodo-tracking`, con el central levantado). Por defecto se conecta
a `localhost:61617`, el puerto con TLS del broker, y confía en el certificado de desarrollo de
`tls/confianza.p12`:

    mvn -q -DskipTests package
    $env:NODO_ID="tracking-1"; $env:NODO_JMS_USUARIO="tracking-1"; $env:NODO_JMS_CLAVE="tracking123"
    java -cp "target/nodo-tracking.jar;target/lib/*" tse.cargauy.nodotracking.NodoTracking

## Configuración

| Variable | Por defecto | Para qué |
| --- | --- | --- |
| `NODO_ID` | obligatoria | Identificador del nodo registrado en el central |
| `NODO_JMS_USUARIO` / `NODO_JMS_CLAVE` | obligatorias | Usuario del nodo en el broker (`tracking-1` / `tracking123` en desarrollo) |
| `NODO_JMS_URL` | `tcp://localhost:61617?sslEnabled=true&trustStorePath=tls/confianza.p12&trustStorePassword=cargauy-dev` | Dirección del broker, con TLS |
| `NODO_PUERTO` | `8081` | Puerto de `/health` y `/metricas` |
| `NODO_MAX_LOTE` | `500` | Máximo de eventos por publicación |
| `NODO_RUTAS` | `rutas-prueba.json` | Archivo JSON con la flota (admite `desvioKm` por vehículo) |
| `NODO_VEHICULOS_EXTRA` | `0` | Vehículos `SIM0001...` con rutas al azar, para la prueba de carga |
| `NODO_INTERVALO_SEG` | `10` | Cada cuánto reporta cada vehículo. 10 s para desarrollo; la prueba de performance usa 30 s, la frecuencia del [volumen objetivo](../docs/performance.md#volumen-objetivo-de-tracking) |
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

## Cómo ver lo que hace

- `GET http://localhost:8081/health`: `200 {"estado":"UP"}` si está conectado al broker; si no,
  `503 {"estado":"DOWN"}`.
- `GET http://localhost:8081/metricas`: generados, duplicados, publicados, pendientes y reintentos.
  Las mismas métricas van por OTLP a la Plataforma de Observabilidad, junto con una traza por cada
  lote publicado (el contexto viaja en la propiedad `traceparent` de cada mensaje) y los logs.
  Las mismas métricas quedan en el log cada `NODO_LOG_METRICAS_SEG` segundos
  (`docker compose logs nodo-tracking`).
- Ruta de un viaje en el central: en el frontend, como funcionario, **Ver recorrido** en la grilla de viajes
- Qué hizo la ingesta con cada evento: `docker compose logs carga-uy | grep ingesta`

Al detenerlo (`docker compose stop nodo-tracking`) publica lo pendiente e imprime las métricas
finales. Para contar las posiciones almacenadas en MongoDB, desde `carga-uy`:

    docker compose exec mongo mongosh -u cargauy -p cargauy --quiet --eval 'db.getSiblingDB("cargauy").posiciones.countDocuments()'

El recuento depende también de los datos anteriores, los eventos rechazados y la deduplicación;
no equivale por sí solo al contador `generados`. Para correlacionar lo publicado con lo procesado,
usar las métricas y trazas de la [documentación de observabilidad](../docs/observabilidad.md).

## Despliegue en OpenShift

El nodo corre en un proyecto de OpenShift distinto al del central (R15) y publica en el broker del
central por la Route `artemis-nodos`, de tipo *passthrough* en el puerto 443: TLS de punta a punta, con
el destino indicado por SNI. No recibe conexiones, así que no necesita Service ni Route propios. El
manifiesto es [openshift/nodo-tracking.yaml](openshift/nodo-tracking.yaml).

Antes de empezar, el nodo tiene que estar registrado y habilitado en el central con su identificador
(CU-19); si no, el central descarta sus eventos.

1. **Imagen.** El workflow [Publicar tracking](../.github/workflows/publicar-tracking.yml) publica
   `ghcr.io/lucaslazogue/tse-carga-uy/nodo-tracking` (`latest` y el SHA) en cada push a `release` que
   modifica el nodo, o a mano desde Actions. El paquete tiene que ser público para que OpenShift lo
   descargue sin credenciales.
2. **Sesión.** Iniciar sesión en el proyecto del nodo como se describe en
   [acceso a OpenShift](../docs/infraestructura.md#acceso-a-openshift). Si se trabaja con otros clústeres,
   conviene un `KUBECONFIG` aparte para no cambiar el contexto activo.
3. **Truststore.** El broker usa un certificado propio. El script lo baja por la Route y arma
   `confianza.p12` con la clave que se elija:

       NODO_TRUSTSTORE_CLAVE=CLAVE_DEL_TRUSTSTORE sh openshift/truststore.sh HOST_DE_LA_ROUTE

   Imprime la huella SHA-256 del certificado. Antes de usar el archivo, compararla con la del keystore
   del broker (`keytool -list -v -keystore artemis.p12`), que está en el Secret de Artemis del proyecto
   del central.
4. **Secret.** La clave del usuario del nodo en el broker, la del truststore y el archivo:

       oc create secret generic nodo-tracking-secretos \
         --from-literal=NODO_JMS_CLAVE=CLAVE_DEL_NODO \
         --from-literal=NODO_TRUSTSTORE_CLAVE=CLAVE_DEL_TRUSTSTORE \
         --from-file=confianza.p12

5. **Despliegue.** El manifiesto fija el host del broker, `tracking-1`, la flota sur y la frecuencia
   del volumen objetivo (30 s), y deja apagado OpenTelemetry hasta que haya colector en la nube:

       oc apply -f openshift/nodo-tracking.yaml
       oc rollout status deployment/nodo-tracking
       oc logs deploy/nodo-tracking

   Para ver la salud y las métricas, `oc port-forward deploy/nodo-tracking 8081:8081` y abrir
   `http://localhost:8081/health`. Mientras no hay conexión con el broker, `/health` da `503` y el pod
   no queda listo, pero no se reinicia: el nodo guarda los eventos y reintenta solo.

Para detenerlo sin borrarlo, `oc scale deployment/nodo-tracking --replicas=0`.

## Tests

    mvn verify

`mvn test -Dtest=PruebaManualPublicador` publica dos eventos en el central real (necesita el central
levantado y `tracking-1` habilitado).
