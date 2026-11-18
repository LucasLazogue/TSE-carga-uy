# Performance

[Proyecto](../README.md) · [Observabilidad](observabilidad.md) · [Mensajería](mensajeria.md)

## Volumen objetivo de tracking

El informe ([Vista de Atributos de Calidad](informe/vista-atributos-calidad.tex), AC08) fija el volumen:

| | Valor | De dónde sale |
| --- | --- | --- |
| Flota profesional | 17.869 vehículos con motor (2019) | Registro del MTOP, en el cuadro 2.1 del informe del BID (2022) |
| Vehículos reportando a la vez | **18.000** | Peor caso: toda la flota con un viaje en curso, redondeado |
| Frecuencia de reporte | una posición cada 30 s | Requisito de carga.uy: ~670 m entre posiciones a 80 km/h |
| Volumen objetivo | **600 posiciones/s** | 18.000 / 30 |
| Pico (×3) | **1.800 posiciones/s** | Vehículos que recuperan cobertura y descargan lo acumulado |

La justificación completa, y por qué la estimación queda del lado alto, está en la sección "Volumen
objetivo de tracking" del informe.

## Cómo generar la carga

El nodo de tracking suma vehículos simulados (`SIM0001`, `SIM0002`…) con rutas al azar.
`NODO_VEHICULOS_EXTRA=N` con `NODO_INTERVALO_SEG=S` genera unas `N / S` posiciones por segundo.
El intervalo por defecto del compose es de 10 s, para que en desarrollo y en la demo el recorrido
de la flota de prueba se vea rápido. La prueba de performance usa el volumen del informe: 18.000
vehículos cada 30 s, repartidos entre los dos nodos. Desde `carga-uy`:

    NODO_INTERVALO_SEG=30 NODO_VEHICULOS_EXTRA=9000 NODO_2_VEHICULOS_EXTRA=9000 \
      docker compose up -d nodo-tracking nodo-tracking-2

Para el pico, `NODO_SIN_COBERTURA=desde,durante` (en minutos) retiene las posiciones y las publica
todas juntas al volver la cobertura. Para volver a la flota normal, `NODO_VEHICULOS_EXTRA=0`.

Se mira en Grafana, en el tablero **carga.uy · ingesta, nodo y API**:
- posiciones generadas en el nodo frente a procesadas en el central;
- la cola de tracking, que tiene que estabilizarse y no crecer sin límite;
- la espera en la cola y la antigüedad de cada posición al procesarse;
- el tiempo de procesamiento.

Las matrículas simuladas no están registradas: se guardan sin vehículo y no pasan por la
fiscalización, así que la carga real por posición es algo mayor que la medida.

## Usuarios simulados del frontoffice (k6)

Mientras el generador de tracking carga la ingesta, `performance/frontoffice.js` simula con
[k6](https://k6.io) a los usuarios del frontoffice, para verificar el objetivo de p95 < 500 ms en
sus consultas (AC09). Tiene tres escenarios con llegada constante de pedidos (`TASA` iteraciones por
segundo cada uno):

- **Responsable de empresa:** sus viajes, el detalle y los eventos de uno, sus vehículos y sus infracciones.
- **Funcionario:** empresas, vehículos y casos de fiscalización.
- **Público, sin sesión:** validación de una empresa e indicadores del año.

Los tokens no pasan por gub.uy: el script los firma con el mismo secreto del central
(`CARGAUY_JWT_SECRET`) y las mismas claims que `UsuarioEJB`. Los usuarios salen de la base:

```bash
KUBECONFIG=~/.kube/demo-central performance/usuarios.sh   # genera performance/usuarios.json (no se versiona)
k6 run -e JWT_SECRETO="$CARGAUY_JWT_SECRET" -e TASA=10 -e DURACION=10m performance/frontoffice.js
```

Los umbrales de k6 fallan la corrida si el p95 de algún escenario pasa de 500 ms o si más del 1 %
de los pedidos fallan. `BASE_URL` cambia el destino (por defecto, el central en OpenShift).

## Prueba preliminar (26/10/2026, máquina de desarrollo)

Todo el compose en una sola Mac de 8 núcleos y 8 GB para Docker: el nodo de tracking que genera la
carga, el broker, el central, las dos bases y los nodos de balanzas. Escalones de unos 2,5 minutos,
con el intervalo de 10 s del compose: cada escalón llega a la misma carga que el triple de vehículos
reportando cada 30 s.

| Vehículos | Objetivo (pos/s) | Generadas (pos/s) | Procesadas (pos/s) | Cola (máx.) | Espera p95 | Procesamiento p95 |
| --- | --- | --- | --- | --- | --- | --- |
| 1.000 | 100 | 85 | 88 | 1.002 | 10,9 s | 38 ms |
| 2.500 | 250 | 250 | 279 | 2.192 | 12,6 s | 8,4 ms |
| 5.000 | 500 | 500 | 521 | 9.932 | ≥ 30 s | 7,7 ms |

- Hasta 250 posiciones/s el central procesa lo que llega y la cola se mantiene acotada.
- En 500 posiciones/s procesa en promedio lo que llega, pero la cola oscila entre 6.000 y 11.000
  mensajes y la espera llega al tope del histograma (30 s). Al sostenerlo, la máquina se satura
  (load average de 20 en 8 núcleos): el propio generador baja a ~230 posiciones/s y la ingesta cae.
- Cada posición se procesa en menos de 8 ms (p95). El cuello de botella observado no es la ingesta
  sino la CPU compartida por todos los contenedores, el generador incluido.
- El primer escalón arrancó con la cola de una prueba anterior; sus esperas no son representativas.

Conclusión: en la máquina de desarrollo el punto de quiebre está entre 250 y 500 posiciones/s. Para
la prueba de performance de la entrega final (AC09 a AC11) hay que separar el generador del central
y correrla con el central en Elastic Cloud. Las varias instancias del central consumen la misma cola
(DA02), así que se puede medir también el escalado horizontal.

## Conexiones a la base

Cada instancia del central tiene su propio pool de conexiones a PostgreSQL (`CargaUyDS`), así que al
escalar horizontalmente las conexiones se multiplican: el total es el pool por instancia por la cantidad
de instancias, y tiene que quedar por debajo del `max_connections` de PostgreSQL (100 en OpenShift, de
las que 3 quedan reservadas al superusuario).

| Parámetro (variable de entorno) | Valor por defecto | Qué limita |
| --- | --- | --- |
| `DB_POOL_MAX` | 20 | Conexiones abiertas por instancia |
| `DB_POOL_MIN` | 2 | Conexiones que la instancia mantiene abiertas aunque no las use |
| `DB_POOL_ESPERA_MS` | 5.000 | Cuánto espera un pedido por una conexión libre antes de fallar |
| `TRACKING_CONSUMIDORES` | 15 | Consumidores concurrentes de la cola de tracking por instancia (`maxSession` del MDB) |

Con el autoescalado de 1 a 4 instancias, 4 × 20 = 80 conexiones, más las de las herramientas
(`psql`, el script de reinicio). Una quinta instancia ya pasaría el límite y PostgreSQL rechazaría las
conexiones nuevas ("too many clients"). Antes de llegar a eso aparece el otro síntoma: cuando una
instancia usa todo su pool, los pedidos siguientes esperan una conexión y la latencia sube aunque la
CPU no esté saturada.

Cada consumidor de la cola de tracking ocupa una conexión mientras procesa una posición, así que
`TRACKING_CONSUMIDORES` tiene que quedar por debajo de `DB_POOL_MAX` para que la API REST y el
backoffice no se queden sin conexiones durante un pico de tracking. Además, WildFly limita las
instancias de MDB con su pool `mdb-strict-max-pool` (por defecto, 4 por procesador disponible), que
puede ser el límite real de la concurrencia de la ingesta.

### Cómo se mide en la prueba

- **Del lado del central:** las estadísticas del pool están activadas y salen por la Plataforma de
  Observabilidad, con la instancia en la etiqueta `instance` (el nombre del pod, por
  `OTEL_RESOURCE_ATTRIBUTES`). En Prometheus: `datasources_pool_in_use_count{name="CargaUyDS"}`
  (conexiones en uso), `datasources_pool_available_count`, `datasources_pool_max_used_count`,
  `datasources_pool_wait_count` (pedidos que esperaron una conexión) y `datasources_pool_max_wait_time`.
  La etiqueta `instance` también separa por instancia los tiempos y contadores de la ingesta
  (`ingesta_*`). Las que exporta el SDK de OpenTelemetry de WildFly (`http_server_request_duration`, JVM) no
  la llevan: ese subsistema no toma `OTEL_RESOURCE_ATTRIBUTES`, así que las series de las instancias se pisan. El p95 del
  frontoffice se mide del lado del cliente, con k6.
- **Del lado de la base:** cada instancia se identifica en `pg_stat_activity` con el nombre de su pod
  (`ApplicationName`):

  ```sql
  SELECT application_name, state, count(*) FROM pg_stat_activity
  WHERE datname = 'cargauy' GROUP BY 1, 2 ORDER BY 1, 2;
  ```

- **Qué se busca:** si al pasar de 1 a 4 instancias el throughput de la ingesta y el p95 del
  frontoffice escalan, o si aparecen esperas por conexión antes de que la CPU se sature.

### Si las conexiones son el cuello de botella

1. Ajustar `DB_POOL_MAX` y `TRACKING_CONSUMIDORES` según lo medido, siempre con instancias × pool por
   debajo del límite.
2. Poner un pooler de conexiones (PgBouncer, en modo transacción) entre las instancias y PostgreSQL: las
   instancias abren muchas conexiones contra el pooler y él las multiplexa sobre pocas conexiones reales,
   así que la cantidad de instancias deja de depender de `max_connections`. En modo transacción hay que
   desactivar las sentencias preparadas del lado del servidor en el driver (`prepareThreshold=0`).
3. Subir `max_connections` con moderación: cada conexión de PostgreSQL es un proceso con su memoria, y
   muchas conexiones activas compitiendo por pocas CPU rinden peor que menos conexiones bien usadas.
