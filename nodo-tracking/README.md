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
| `NODO_MAX_PENDIENTES` | `200000` | Tope de eventos esperando el broker (unos 45 MB). Lleno, descarta los nuevos y los cuenta en `descartados` |
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
- `GET http://localhost:8081/metricas`: generados, duplicados, publicados, pendientes, reintentos y
  descartados.
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

Los dos nodos corren en un proyecto de OpenShift distinto al del central (R15), con la flota repartida
como en el compose: `nodo-tracking-1` (`tracking-1`, flota sur) y `nodo-tracking-2` (`tracking-2`,
flota norte). Publican en el broker del central por la Route `artemis-nodos`, de tipo *passthrough* en
el puerto 443: TLS de punta a punta, con el destino indicado por SNI. Lo único que exponen es `/health`,
por HTTPS, con una Route por nodo (`nodo-tracking-1-salud` y `nodo-tracking-2-salud`), para que la
Plataforma de Observabilidad los consulte como en el compose; `/metricas` no queda expuesto. El
manifiesto es [openshift/nodo-tracking.yaml](openshift/nodo-tracking.yaml).

Antes de empezar, los nodos tienen que estar registrados y habilitados en el central con su
identificador (CU-19); si no, el central descarta sus eventos.

1. **Imagen.** El workflow [Deploy tracking](../.github/workflows/deploy-tracking.yml) publica
   `ghcr.io/lucaslazogue/tse-carga-uy/nodo-tracking` (`latest` y el SHA) en cada push a `release` que
   modifica el nodo, o a mano desde Actions. El paquete tiene que ser público para que OpenShift lo
   descargue sin credenciales.
2. **Sesión.** Iniciar sesión en el proyecto del nodo como se describe en
   [acceso a OpenShift](../docs/infraestructura.md#acceso-a-openshift). Si se trabaja con otros clústeres,
   conviene un `KUBECONFIG` aparte para no cambiar el contexto activo.
3. **Truststore.** El broker usa un certificado propio. El script lo baja por la Route y arma
   `confianza.p12` con la clave que se elija. Lo usan los dos nodos y solo hay que rehacerlo si cambia
   el certificado del broker o la Route:

       NODO_TRUSTSTORE_CLAVE=CLAVE_DEL_TRUSTSTORE sh openshift/truststore.sh HOST_DE_LA_ROUTE

   Imprime la huella SHA-256 del certificado. Antes de usar el archivo, compararla con la del keystore
   del broker (`keytool -list -v -keystore artemis.p12`), que está en el Secret de Artemis del proyecto
   del central.
4. **Secret.** Las claves de los usuarios `tracking-1` y `tracking-2` en el broker, la del truststore,
   el archivo y el token de la [Plataforma de Observabilidad](../docs/observabilidad.md#despliegue-en-openshift)
   (`OTLP_NODOS_TOKEN` del Secret `observabilidad-secretos`, en el proyecto del central):

       oc create secret generic nodo-tracking-secretos \
         --from-literal=TRACKING_1_CLAVE=CLAVE_DE_TRACKING_1 \
         --from-literal=TRACKING_2_CLAVE=CLAVE_DE_TRACKING_2 \
         --from-literal=NODO_TRUSTSTORE_CLAVE=CLAVE_DEL_TRUSTSTORE \
         --from-literal=OTLP_NODOS_TOKEN=TOKEN_DE_LA_PLATAFORMA \
         --from-file=confianza.p12

   Si el Secret ya existe, el token se agrega con
   `oc set data secret/nodo-tracking-secretos OTLP_NODOS_TOKEN=TOKEN_DE_LA_PLATAFORMA`.

5. **Despliegue.** El manifiesto fija el host del broker, los usuarios, las flotas, la frecuencia
   del volumen objetivo (30 s) y el envío de trazas, métricas y logs a la plataforma por la Route
   `observabilidad-otlp`. Si la plataforma no está, el nodo funciona igual y solo registra los errores de
   exportación:

       oc apply -f openshift/nodo-tracking.yaml
       oc rollout status deployment/nodo-tracking-1
       oc logs deploy/nodo-tracking-1

   Si el central todavía no está listo para recibir posiciones, crearlos apagados y encenderlos después
   con `oc scale`:

       sed 's/replicas: 1/replicas: 0/' openshift/nodo-tracking.yaml | oc apply -f -

   La salud se ve en `https://` + `oc get route nodo-tracking-1-salud -o jsonpath='{.spec.host}'` +
   `/health`. Para las métricas, `oc port-forward deploy/nodo-tracking-1 8081:8081` y abrir
   `http://localhost:8081/metricas`. Mientras no hay conexión con el broker, `/health` da `503` y el pod
   no queda listo, pero no se reinicia: el nodo guarda los eventos y reintenta solo.

Cada nodo empieza a publicar apenas arranca. Para detenerlo sin borrarlo,
`oc scale deployment/nodo-tracking-1 --replicas=0`, y `--replicas=1` para volver. Para la [prueba de
carga](../docs/performance.md), los vehículos extra se cambian sin tocar el manifiesto; el pod se
reinicia con el valor nuevo:

    oc set env deployment/nodo-tracking-1 deployment/nodo-tracking-2 NODO_VEHICULOS_EXTRA=7500

### Despliegue automático

Después del primer despliegue, el workflow Deploy tracking actualiza la imagen de los dos nodos en
cada push a `release` que modifica el nodo. Usa la cuenta de servicio `github-deploy` del proyecto de
los nodos ([openshift/despliegue-ci.yaml](openshift/despliegue-ci.yaml)). El mismo token sirve para las
pruebas: encender y apagar los nodos, cambiar la carga y ver pods, logs y métricas; no lee Secrets ni
crea o borra recursos.
No aplica el manifiesto, así que respeta las réplicas y las variables cambiadas con `oc scale` y
`oc set env`; un cambio en el manifiesto se aplica a mano con `oc apply`.

Para prepararlo, una vez:

1. Crear la cuenta de servicio, su rol y su token:

       oc apply -f openshift/despliegue-ci.yaml

2. Copiar el token y cargarlo en el secreto `OPENSHIFT_TRACKING_TOKEN` del repositorio (solo puede
   hacerlo el dueño):

       oc get secret github-deploy-token -o jsonpath='{.data.token}' | base64 -d

Sin ese secreto, el workflow publica la imagen y no despliega. El servidor y el proyecto están en el
workflow.

## Tests

    mvn verify

`mvn test -Dtest=PruebaManualPublicador` publica dos eventos en el central real (necesita el central
levantado y `tracking-1` habilitado).
