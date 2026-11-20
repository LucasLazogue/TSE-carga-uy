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

Para el pico hay dos formas. `NODO_INTERVALO_SEG=10` con la misma flota triplica la carga de forma pareja (1.800
posiciones/s con los 18.000 vehículos): es la que se usó en OpenShift. `NODO_SIN_COBERTURA=desde,durante` (en minutos)
retiene las posiciones y las publica todas juntas al volver la cobertura, que es más fiel al caso real pero llega de
golpe. Para volver a la flota normal, `NODO_VEHICULOS_EXTRA=0`.

Se mira en Grafana, en el tablero **carga.uy · ingesta, nodo y API**:
- posiciones generadas en el nodo frente a procesadas en el central;
- la cola de tracking, que tiene que estabilizarse y no crecer sin límite;
- la espera en la cola y la antigüedad de cada posición al procesarse;
- el tiempo de procesamiento.

Sin el [juego de datos](#juego-de-datos), las matrículas simuladas no están registradas: se guardan sin vehículo y no
pasan por la fiscalización, así que la carga por posición queda por debajo de la real.

## Juego de datos

[performance/datos.sql](../performance/datos.sql) registra en el central la flota del volumen objetivo, para que cada
posición recorra la operativa real: vehículo conocido, viaje en curso, permiso e ITV vigentes, y las reglas de
fiscalización. Con los valores por defecto:

| Qué | Cantidad |
| --- | --- |
| Empresas, cada una con su responsable | 90 |
| Vehículos `SIM0001` a `SIM18000`, con permiso aprobado, ITV, chofer, un viaje en curso y el siguiente asignado | 18.000 |
| Vehículos con la ITV vencida (su primera posición del día abre un caso "sin habilitación") | 180 (1 %) |
| Viajes terminados, con su Guía y sus eventos de inicio y fin: 6 meses de historia | 108.000 |
| Casos de exceso de carga notificados, con su evidencia | 2.160 |
| Funcionarios para el escenario de k6 | 5 |

Los nodos generan exactamente esas matrículas: `tracking-1` desde `SIM0001` y `tracking-2` desde `SIM9001`
(`NODO_VEHICULOS_DESDE`). Las cantidades se cambian con parámetros de `psql` (`-v vehiculos=6000 -v meses=3`;
la lista está en el encabezado del script). Los ids arrancan en 10.000.000, lejos de los que asigna la aplicación, y
las cédulas son inventadas.

Se carga sobre una base con solo los datos de referencia, con el perfil `performance` del reinicio
([datos del central](infraestructura.md#datos-del-central)):

```sh
cd carga-uy/openshift && ./reiniciar-datos.sh --perfil performance     # unos 15 s de carga
```

Para volver a la demo, `./reiniciar-datos.sh`. Como el reinicio parte siempre de una base vacía y el script da
siempre los mismos datos, cada corrida arranca del mismo punto; las fechas son relativas al día de la carga.

## Usuarios simulados del frontoffice (k6)

Mientras el generador de tracking carga la ingesta, `performance/frontoffice.js` simula con
[k6](https://k6.io) a los usuarios del frontoffice, para verificar el objetivo de p95 < 500 ms en
sus consultas (AC09). Tiene tres escenarios con llegada constante de pedidos (`TASA` iteraciones por
segundo cada uno):

- **Responsable de empresa:** sus viajes, el detalle y los eventos de uno, sus vehículos y sus infracciones.
- **Funcionario:** empresas, vehículos y casos de fiscalización.
- **Público, sin sesión:** validación de una empresa e indicadores del año.
- **Móvil (opcional, `TASA_MOVIL` choferes por segundo):** el chofer de un vehículo `SIM` pide sus viajes en la app y
  manda en un mismo envío el fin del viaje en curso y el inicio del siguiente, que el
  [juego de datos](#juego-de-datos) deja asignado. Al terminar el viaje, el central lo evalúa en segundo plano y le pide
  los pesajes a los nodos de balanzas: es el camino que ejercita la fiscalización de exceso de carga y las balanzas
  con carga. Con 18.000 vehículos y viajes de unas 5 horas termina un viaje por segundo (`TASA_MOVIL=1`). Cada
  iteración usa el vehículo siguiente, desde `MOVIL_DESDE` (1 por defecto): en otra corrida sobre la misma carga de
  datos hay que arrancar después de los ya usados, o reiniciar los datos.
- **Consultas de detalle (opcional, `TASA_DETALLE` por segundo para cada perfil):** el funcionario abre el monitoreo de
  un viaje en curso de una empresa (sus posiciones, que salen de MongoDB, y sus pesajes, que se les piden en vivo a los
  nodos de balanzas), el detalle de un caso de su bandeja con la auditoría y sus notificaciones; el responsable abre una
  de sus infracciones con la evidencia y sus notificaciones.
- **Móvil con eventos acumulados (opcional, `TASA_ACUMULADO` choferes por segundo):** un chofer recupera la señal y
  manda en un solo envío `EVENTOS_ACUMULADOS` (20) eventos del viaje en curso guardados sin conexión: cargas y
  descargas parciales e incidentes. Después reenvía el mismo lote, como cuando la app no recibió la respuesta. k6
  verifica que la primera vez se apliquen todos y que la segunda vuelvan como duplicados. Usa los vehículos desde
  `ACUMULADO_DESDE` (9001), lejos de los del escenario móvil.

Los tokens no pasan por gub.uy: el script los firma con el mismo secreto del central
(`CARGAUY_JWT_SECRET`) y las mismas claims que `UsuarioEJB`. Los usuarios salen de la base:

```bash
KUBECONFIG=~/.kube/demo-central performance/usuarios.sh   # genera performance/usuarios.json (no se versiona)
k6 run -e JWT_SECRETO="$CARGAUY_JWT_SECRET" -e TASA=10 -e DURACION=10m performance/frontoffice.js
k6 run -e JWT_SECRETO="$CARGAUY_JWT_SECRET" -e TASA=5 -e TASA_MOVIL=1 -e DURACION=10m performance/frontoffice.js
```

Los umbrales de k6 fallan la corrida si el p95 de algún escenario pasa de 500 ms o si más del 1 %
de los pedidos fallan. `BASE_URL` cambia el destino (por defecto, el central en OpenShift).

Corrida de humo del 18/11/2026 contra OpenShift (`TASA=1`, `DURACION=30s`, una instancia, sin carga de
tracking): 298 pedidos sin errores, p95 de 164 ms (público), 174 ms (responsable) y 206 ms
(funcionario). El pedido más rápido tardó 147 ms, que es casi todo red entre la máquina que corre k6 y el
clúster: conviene anotar ese piso junto con los resultados de la prueba.

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
y correrla con el central en la nube (se hizo en OpenShift: ver la sección siguiente). Las varias instancias del central consumen la misma cola
(DA02), así que se puede medir también el escalado horizontal.

## Prueba en OpenShift (18 y 19/11/2026)

### Objetivos y resultado

| Objetivo (letra, sección 4.3) | Resultado | Dónde |
| --- | --- | --- |
| p95 de las consultas del frontoffice < 500 ms | ✅ En operación normal, con [#145](https://github.com/LucasLazogue/TSE-carga-uy/pull/145): 204 / 278 / 205 ms (funcionario / responsable / público) con 600 pos/s. ⚠️ En el extremo de 5.400 infracciones abiertas a la vez y un fin de viaje por segundo, el responsable llega a ~830 ms | [Listados del frontoffice](#listados-del-frontoffice), [las cuatro reglas a la vez](#las-cuatro-reglas-a-la-vez) |
| Sostener el volumen estimado (600 pos/s) sin pérdida | ✅ procesa lo que llega, cola estable y DLQ en 0 (los únicos errores son reentregas al apagarse una instancia) | [Resultados](#resultados) |
| Los tiempos no se degradan a lo largo de la prueba | ✅ 40 minutos con p95 estable | [Corrida larga](#sin-degradación-en-el-tiempo) |
| Punto de quiebre | ~1.600 pos/s con 4 instancias, limitado por la CPU de PostgreSQL | [Escalado](#escalado-horizontal) |
| Cuellos de botella, con evidencia de la observabilidad | Índices, memoria y CPU de PostgreSQL, una consulta pública repetida, MongoDB, CPU del central | [Qué limitó cada corrida](#qué-limitó-cada-corrida) |
| Pico ×3 (1.800 pos/s) | Se encola y se procesa con atraso, sin pérdida | [Pico](#pico-de-1800-posicioness) |
| Balanzas indisponibles | El central sigue respondiendo; el viaje queda pendiente y se reevalúa al volver el nodo | [Escenarios de falla](#escenarios-de-falla) |
| PDI que no responde | El ingreso sigue funcionando con el perfil pendiente | [Escenarios de falla](#escenarios-de-falla) |
| Consultas de detalle y sincronización del móvil con eventos acumulados | 16 lecturas del frontoffice con p95 de 177 a 276 ms y 0 errores con 600 pos/s; 600 lotes de 20 eventos aplicados y sus reenvíos como duplicados | [Consultas de detalle](#consultas-de-detalle-y-móvil-con-eventos-acumulados) |
| Las cuatro reglas de fiscalización con carga | Sin habilitación, viaje sin guía y desvío (1.800 casos cada una) y exceso de carga (unos 250) se abren con 600 pos/s sin afectar la ingesta | [Las cuatro reglas a la vez](#las-cuatro-reglas-a-la-vez) |
| Escalado horizontal efectivo (opcional 4) | 1 → 2 instancias: ×1,9; 2 → 4: ×1,4 | [Escalado](#escalado-horizontal) |

### Entorno

| Componente | Recursos (límite) | Notas |
| --- | --- | --- |
| Central (WildFly) | 2 CPU, 2 GiB por instancia | Autoescalado de 1 a 4 instancias por CPU (70 %) |
| PostgreSQL | 1 CPU y 512 MiB al empezar; 2 CPU y 2 GiB; **4 CPU y 2 GiB** desde la corrida 4; 4 CPU y 3 GiB en la última corrida del 20/11 | Queda en 4 CPU y 3 GiB ([infraestructura](infraestructura.md)) |
| MongoDB | 1 CPU; **2 CPU** desde la prueba de escalado; 1 GiB y 5 GiB de disco | Queda en 2 CPU |
| Artemis | 1 CPU, 2,5 GiB | Cola `queue_tracking` |
| Nodos de tracking (2) | 1 CPU cada uno | En otro proyecto del mismo clúster; publican al Artemis del central por su ruta |
| Nodos de balanzas (2) | | En otro clúster de OpenShift; el central les pregunta por HTTPS |
| k6 | Máquina de desarrollo | Unos 280 ms de cada pedido son TLS y red hasta el clúster |

La base se carga con el [juego de datos](#juego-de-datos) antes de cada corrida (perfil `performance`). La carga de
tracking se sube por escalones con `NODO_VEHICULOS_EXTRA` (2.250 vehículos extra por nodo = 150 posiciones/s en total)
y el frontoffice se simula con k6 con `TASA=5` (15 iteraciones/s entre los tres escenarios, unos 50 pedidos/s). Cada
15 s se registran las métricas de Prometheus (generadas, publicadas y procesadas por segundo, cola, tiempos de la
ingesta, uso del pool) y la CPU de cada pod (`oc adm top`); cuando hace falta, también qué consultas están activas en
`pg_stat_activity`. Los datos crudos de cada corrida (decenas de MB) no se versionan.

### Resultados

| Corrida | Cambios | Carga | Procesadas (pos/s) | Frontoffice p95 | Errores k6 | CPU PostgreSQL |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Ninguno: PostgreSQL con el límite por defecto (1 CPU, 512 MiB) | 150 pos/s | ~14 | | | 1 CPU (tope); reinicios por memoria (OOMKilled) |
| 2 | Índices de claves foráneas; PostgreSQL a 2 CPU y 2 GiB | 150 / 300 / 600 pos/s | 150 / 300 / 600 | | | 0,44 / 0,85 / 1,7–2 CPU |
| 2 | | 600 pos/s + k6 | ~22 | 8,5 s / 9,3 s / 20,7 s | 10 % | 2 CPU (tope) |
| 2 | | k6 solo | | 6,5 s / 7,2 s / 18,1 s | 8,9 % | 2 CPU (tope) |
| 3 | Caché de indicadores públicos e índice de guías por empresa | k6 solo | | 296 / 258 / 163 ms | 0 % | |
| 3 | | 600 pos/s + k6 | ~460 | 1,9 s / 2,4 s / 3,4 s | 2,4 % | 2 CPU (tope) |
| 4 | PostgreSQL a 4 CPU | 600 pos/s + k6 | **600** | **347 / 351 / 168 ms** | **0 %** | ~2,2 CPU |
| 4 | | 900 pos/s | **900** | | | ~2,1 CPU |
| 5 | Ninguno (corrida larga) | 600 pos/s + k6, 40 min | **600** | **388 / 426 / 186 ms** | **0 %** (120.000 pedidos) | 2,2–2,35 CPU |
| 6 | Regla de desvío de ruta, caché de normas, escenario móvil | 600 pos/s + k6 + 1 fin de viaje/s, 12 min, con `balanzas-norte` caída 4 min | **600** | **406 / 398 / 192 ms** | **0 %**; móvil: 100 % aplicados | 2,5–2,9 CPU |

Los p95 del frontoffice son de los escenarios funcionario / responsable / público. El tiempo de procesamiento de una
posición (p95) fue de 13 ms a 150 pos/s, 65 ms a 300 y entre 50 y 80 ms a 600 con k6; la espera en la cola (p95), de
unos 350 a 490 ms a 600 pos/s y 600 ms a 900. El autoescalado llevó el central a 4 instancias en todas las corridas
con carga. En la corrida 6 la base usa más CPU que en la 4 y la 5: suma la regla de desvío (la Guía del viaje con cada
posición) y la evaluación de un viaje terminado por segundo; con el límite de 4 CPU queda margen.

### Qué limitó cada corrida

1. **Faltaban índices.** PostgreSQL no indexa las claves foráneas, y la ingesta busca el viaje en curso, el permiso y
   la ITV por vehículo en cada posición. Sin índice, la búsqueda del viaje recorría la tabla entera (24 ms); con el
   índice, 0,1 ms. Se agregaron como `@Index` en las entidades.
2. **El límite por defecto del proyecto (1 CPU, 512 MiB) no alcanza para PostgreSQL.** Con 18.000 vehículos, la base
   se quedaba sin memoria y el contenedor se reiniciaba. Se subió a 2 CPU y 2 GiB
   ([infraestructura](infraestructura.md)).
3. **Una consulta pública repetida.** Con k6, la consulta de los indicadores públicos (agregados de meses sobre todos
   los viajes) era la que más aparecía en `pg_stat_activity`: se calculaba entera en cada pedido, aunque el resultado
   es el mismo para cualquiera que pida el mismo período. Le sacaba a la base la CPU que necesitaba la ingesta, que
   bajaba a 22 pos/s. Ahora cada instancia guarda el resultado 5 minutos (`CacheIndicadoresPublicos`). También
   faltaba un índice en `guia(empresa_id)` para el listado de viajes de una empresa.
4. **La CPU de PostgreSQL.** Con 2 CPU la base quedaba en su tope con 600 pos/s y k6 juntos; con 4 CPU usa unas 2,2 y
   se cumplen los dos objetivos a la vez.
5. **Por encima del volumen objetivo**, según la configuración: MongoDB en su límite de 1 CPU (pico), la CPU del
   central con una o dos instancias, y otra vez PostgreSQL con cuatro ([escalado](#escalado-horizontal)).
6. **Tres listados del frontoffice con una consulta por fila.** Casos, infracciones y viajes de la empresa hacían entre
   38 y 103 consultas por pedido y eran la cola del p95 en todas las corridas; con cada vez más casos y viajes, el p95
   de funcionario y responsable pasaba de 500 ms según la sesión. Corregido en #145
   ([listados del frontoffice](#listados-del-frontoffice)).

### Sin degradación en el tiempo

Corrida 5: 600 pos/s y k6 durante 40 minutos. p95 del frontoffice por tramos de 5 minutos:

| Minutos | Funcionario | Responsable | Público |
| --- | --- | --- | --- |
| 0–5 | 476 ms | 527 ms | 210 ms |
| 5–10 | 397 ms | 432 ms | 188 ms |
| 10–15 | 379 ms | 422 ms | 185 ms |
| 15–20 | 368 ms | 396 ms | 180 ms |
| 20–25 | 381 ms | 415 ms | 183 ms |
| 25–30 | 380 ms | 404 ms | 180 ms |
| 30–35 | 363 ms | 392 ms | 179 ms |
| 35–40 | 366 ms | 413 ms | 182 ms |

![Tablero de la ingesta durante la corrida larga](imagenes/performance/corrida-larga-40-min.jpg)

*Corrida 5 en el tablero de la ingesta: 600 pos/s parejas durante 40 minutos, con el procesamiento y la espera en la
cola planos después del arranque y 4 instancias. El único error, al principio, es una transacción deshecha cuando el
autoescalado apagó una instancia: el broker reentrega el mensaje a otra y no llega a la DLQ
([infraestructura](infraestructura.md#varias-instancias-y-autoescalado)).*

El primer tramo es más lento por el arranque (compilación de la JVM, cachés vacías y el autoescalado subiendo
instancias); después los tiempos quedan estables, sin tendencia a crecer. La ingesta se mantuvo en 600 pos/s todo el
tiempo, con el procesamiento en unos 80 ms (p95) y la CPU de PostgreSQL entre 2,2 y 2,35.

### Pico de 1.800 posiciones/s

Con PostgreSQL en 4 CPU, la flota del volumen objetivo reportó cada 10 s durante 5 minutos (1.800 pos/s) y después
volvió a cada 30 s (600 pos/s):

- Durante el pico el central procesó unas **950 pos/s**: la diferencia se acumuló en la cola de Artemis, que llegó a
  unos **240.000 mensajes**, sin errores ni mensajes en la DLQ.
- De vuelta a 600 pos/s, la cola bajó unos 340 mensajes por segundo (procesando ~940): se vacía en unos 12 minutos.
  Mientras tanto, la antigüedad de las posiciones al procesarse pasa del tope del histograma (30 s).
- El límite de esas 950 pos/s no fue PostgreSQL (2,4 de sus 4 CPU) ni el central (unos 3 CPU entre las 4 instancias):
  MongoDB estuvo todo el pico en su límite de 1 CPU. En `pg_stat_activity`, unas 8 conexiones por instancia estaban
  `idle in transaction` y casi ninguna ejecutando: son los 8 consumidores de cada instancia (`mdb-strict-max-pool`) con
  la transacción abierta mientras guardan la posición en MongoDB. El pool de conexiones no es el límite: está lleno
  porque cada consumidor retiene su conexión mientras espera.

![Tablero de la ingesta en la corrida 4 y el pico](imagenes/performance/corrida4-600-900-y-pico-1800.jpg)

*Corrida 4 y el pico: 600 y 900 pos/s sin cola; desde las 01:26, el pico de 1.800 pos/s procesado a unas 950 mientras
la cola sube a ~240.000 mensajes y después baja. La espera y la antigüedad quedan en el tope del histograma (30 s)
mientras hay cola.*

La cola cumple su función: el pico no se pierde ni tira abajo al central, se procesa con atraso. Con MongoDB en 2 CPU,
4 instancias procesan unas 1.600 pos/s ([escalado](#escalado-horizontal)): un pico de 1.800 casi no se acumula.

### Escalado horizontal

Con PostgreSQL en 4 CPU y MongoDB en 2, el autoescalado fijo en 1, 2 o 4 instancias y la cola con más mensajes de los
que se pueden procesar (llenada a 1.800 o 2.700 pos/s), lo que procesa el central es su capacidad:

| Instancias | Procesadas (pos/s) | Procesamiento p95 | Límite | CPU central / PostgreSQL / MongoDB |
| --- | --- | --- | --- | --- |
| 1 | ~610 | 12 ms | CPU del central | 1,9 (de 2) / 1,2 / 0,5 |
| 2 | ~1.170 (×1,9) | 60–75 ms | CPU del central | 3,8 (de 4) / 2,6 / 1,1 |
| 4 | ~1.600 (×1,4) | ~50 ms | CPU de PostgreSQL | 4,5 (de 8) / 3,9 (de 4) / 1,6 |

![Tablero de la ingesta con 1, 2 y 4 instancias](imagenes/performance/escalado-1-2-4-instancias.jpg)

*Escalado: las tres mesetas de posiciones procesadas son 1, 2 y 4 instancias (unas 610, 1.170 y 1.600 pos/s), cada
una con la cola llena. El panel de instancias cuenta las que procesaron algo en el último minuto, no las que fijó el
autoescalado.*

Hasta 2 instancias el límite es la CPU de cada instancia y agregar una casi duplica la capacidad. Con 4, el límite
pasa a ser PostgreSQL, que comparten todas: ahí agregar instancias ya no rinde lo mismo. Las conexiones no limitaron la
cantidad de instancias (4 × 20 = 80, por debajo de las 100 de PostgreSQL), pero ver [el pool de cada
instancia](#el-pool-de-cada-instancia-en-la-prueba). El paso siguiente para escalar más sería la base:
más CPU, una réplica de lectura para el frontoffice, o sacar lecturas de la ingesta con cachés como la de normas.

### Escenarios de falla

**Balanzas indisponibles.** `balanzas-norte` se dejó sin responder (su punto de acceso apuntando a una dirección que
no contesta) mientras se pedían pesajes de viajes, a 2 pedidos/s con una instancia:

| | Antes (#131 y #134) | Después |
| --- | --- | --- |
| Pedidos de pesajes | Los primeros tardaban 30 s (504 de la ruta); después todos 500 durante 4 minutos | Todos 200 |
| Causa | Cada pedido retenía su conexión a la base mientras esperaba al nodo y agotaba el pool: fallaban también las validaciones de acceso de otros pedidos | El pedido no tiene transacción abierta mientras consulta |
| Durante la caída | | Unos 15 s de hasta 5 s mientras se acumulan fallas; después se abre el corte y responden en ~450 ms (red) con el nodo informado como sin responder. Cada 60 s un pedido tarda 3 s al probar si el nodo volvió |
| Al volver el nodo | Unos 30 s de errores | Normal en menos de 30 s |

En la corrida 6 la caída fue con la carga completa: el frontoffice no se movió, los viajes terminados durante la caída
quedaron pendientes de evaluar (hasta 251) y el job de reevaluación los completó en unos 4 minutos y medio después de
que volvió el nodo. En esta prueba el corte era uno solo para todas las balanzas: mientras estaba abierto tampoco se le preguntaba al
nodo que funcionaba. Ahora hay uno por nodo ([balanzas](balanzas.md#tolerancia-a-fallas)).

![Tablero de la ingesta en la corrida 6](imagenes/performance/corrida-final-600-k6-movil-balanza-caida.jpg)

*Corrida 6 (600 pos/s, k6, el móvil y `balanzas-norte` caída 4 minutos): la ingesta no cambia durante la caída del
nodo; la cola del arranque se vacía en unos 3 minutos, cuando el autoescalado llega a 4 instancias.*

**PDI que no responde.** En el compose local, con el ingreso simulado de gub.uy (`GUBUY_MOCK=true`): un ingreso de un
ciudadano nuevo por segundo (cada uno consulta la PDI) y una consulta pública por segundo, con el suplantador pausado
(`docker pause`: la conexión se acepta pero nunca llega respuesta) durante 3 minutos:

- Los primeros ingresos tardaron 7 s (dos intentos de 3 s y 1 s entre ellos); después se abre el corte y tardan
  ~14 ms, con el perfil pendiente: los datos de la PDI se completan en el próximo ingreso. Cada 30 s un ingreso tarda
  ~4 s al probar si la PDI volvió.
- El 100 % de los ingresos funcionó, y la consulta pública siguió en ~19 ms (p95 < 70 ms) todo el tiempo.
- Al volver la PDI, los ingresos vuelven a consultarla enseguida.

### Ráfaga de casos

¿Abrir casos en masa afecta al resto? La clave de deduplicación de "sin habilitación" es por vehículo y por día: a
medianoche, todos los vehículos con el permiso o la ITV vencidos abren su caso del día nuevo con su primera posición.
El 19/11 se simuló con 600 pos/s y k6 estables, venciendo por SQL la ITV del 10 % de la flota y 5 minutos después la de
otro 40 % ([prueba-openshift/rafaga_casos.sh](../performance/prueba-openshift/rafaga_casos.sh)):

| Tramo | Casos abiertos | Procesadas (pos/s) | Procesamiento p95 | Espera por conexión | CPU PostgreSQL | Frontoffice p95 |
| --- | --- | --- | --- | --- | --- | --- |
| Antes | | 604 | 42 ms | 3,2 ms | 2,9 | 581 / 580 / 196 ms |
| Ráfaga 1 | 1.800 en unos 30 s | 600 | 45 ms | 3,4 ms | 2,9 | 538 / 575 / 191 ms |
| Ráfaga 2 | 7.200 en unos 45 s | 602 | 47 ms | 3,4 ms | 3,3 (pico 3,5) | 581 / 614 / 199 ms |
| Después, con la mitad de la flota con caso | | 599 | 49 ms | 3,5 ms | 3,4 | 588 / 629 / 206 ms |

Abrir los casos no se nota: la ingesta siguió en 600 pos/s sin errores, el procesamiento subió unos milisegundos y el
pool no cambió. El frontoffice ya estaba por encima de 500 ms antes de las ráfagas porque la corrida arrancó con la base
recién cargada (como el primer tramo de la [corrida larga](#sin-degradación-en-el-tiempo)).

Lo que sí pesa es lo que viene después. Cada posición de un vehículo que ya tiene su caso del día vuelve a evaluar la
regla, encuentra el caso por la clave (0,04 ms con el índice) y lo carga entero con sus relaciones para devolverlo, aunque
el motor no lo usa. Con la base ya caliente, dos controles de 5 minutos con 600 pos/s y k6:

| Control | CPU PostgreSQL | Frontoffice p95 (funcionario / responsable / público) |
| --- | --- | --- |
| La mitad de la flota con caso abierto | 3,3 a 3,7 | **734 / 762 / 254 ms** |
| Las ITV vigentes de nuevo | ~2,6 | 371 / 379 / 199 ms |

Una flota con la mitad de los vehículos en infracción es un extremo, pero el costo es proporcional: cada vehículo con
caso abierto suma esas consultas con cada posición, todo el día.

Se corrigió en [#141](https://github.com/LucasLazogue/TSE-carga-uy/pull/141) sin tocar el criterio de las reglas: si el
caso ya existe, el motor pregunta solo su id. En el compose local, con todos los vehículos con caso abierto, las
consultas por posición bajaron de 25,8 a 19,8: las del caso pasan de 5 (el caso, su descargo, su resolución, la empresa
y el vehículo) a 1. Con #141 desplegado se repitieron los controles esa noche
([prueba-openshift/control_casos.sh](../performance/prueba-openshift/control_casos.sh)), con 5 minutos de k6 para
calentar la base recién cargada y después alternando las dos situaciones dos veces:

| Control, con #141 | CPU PostgreSQL | Frontoffice p95 (funcionario / responsable / público) |
| --- | --- | --- |
| La mitad de la flota con caso abierto | 2,8 | 507 / 568 / 211 ms |
| Las ITV vigentes de nuevo | 2,8 | 512 / 572 / 217 ms |
| Las ITV vigentes (repetición) | 2,8 | 593 / 624 / 242 ms |
| La mitad de la flota con caso abierto (repetición) | 2,8 | 578 / 629 / 222 ms |

Con el arreglo, la mitad de la flota con caso abierto cuesta lo mismo que ningún caso: la CPU de PostgreSQL y los tiempos
del frontoffice son iguales en las dos situaciones, las dos veces.

Lo que no se repitió es la línea de base. Esa noche, sin casos abiertos, funcionario y responsable dieron entre 510 y
630 ms, contra 371 / 379 ms de la tarde, con PostgreSQL unos 0,3 CPU más arriba en todas las fases. La mediana fue la
misma (unos 155 ms): lo que se mueve es la cola de la distribución. Con 600 pos/s y k6 a la vez, el p95 de esos dos
escenarios queda cerca de los 500 ms y cambia de una sesión a otra. La causa apareció al día siguiente, con el tiempo de
cada pedido: ver [listados del frontoffice](#listados-del-frontoffice).

### Listados del frontoffice

El resumen de k6 separa los tiempos por escenario, no por pedido. Con el detalle de cada pedido (`--out csv`, analizado
con [pedidos_k6.py](../performance/prueba-openshift/pedidos_k6.py), salida en
[datos/pedidos.txt](../performance/prueba-openshift/datos/pedidos.txt)), la cola de funcionario y responsable es de
tres listados, en todas las corridas, incluida la larga, que pasaba el umbral en el total del escenario:

| Corrida (p95, ms) | `casos` | `infracciones` | `viajes` de la empresa | Los otros seis pedidos |
| --- | --- | --- | --- | --- |
| Corrida larga (18/11) | 503 | 553 | 491 | 143 a 203 |
| Control con la mitad de la flota con caso abierto (19/11) | 947 | 939 | 1.028 | 145 a 309 |
| Control con las ITV vigentes (19/11) | 423 | 436 | 429 | 173 a 210 |

Contando las consultas por pedido en el compose local, con el juego de datos de performance y `pg_stat_statements`:

| Pedido | Consultas antes | Consultas con #145 | Tiempo antes | Tiempo con #145 |
| --- | --- | --- | --- | --- |
| `casos` (funcionario) | 103 | 6 | 29,5 ms | 9,2 ms |
| `infracciones` (responsable) | 70 | 8 | 34,3 ms | 12,1 ms |
| `viajes` de la empresa (responsable) | 38 | 9 | 29,9 ms | 21,2 ms |
| Los otros seis | 3 a 7 | 3 a 7 | 5 a 9 ms | 5 a 9 ms |

- **Casos e infracciones** salen de la misma búsqueda. Por cada uno de los 20 casos de la página, Hibernate buscaba
  aparte el vehículo, la empresa, el viaje, el descargo y la resolución: el resultado se leía como stream, que carga fila
  por fila, y el descargo y la resolución son `@OneToOne` del lado `mappedBy`, que se buscan de a uno.
- **Viajes:** los roles del chofer, el rubro y el tipo de carga, por fila. Además, el total de la página recorría los
  144.000 viajes (20 ms) para contar los 1.600 de una empresa.

[#145](https://github.com/LucasLazogue/TSE-carga-uy/pull/145) carga las relaciones por lotes (`hibernate.default_batch_fetch_size`), trae el descargo y la
resolución en la misma consulta y cuenta `guia_id`, que PostgreSQL resuelve con el índice único (1,7 ms). Las respuestas
son las mismas: 33 respuestas de los tres listados (552 filas) dieron el mismo JSON antes y después.

Con el central de la nube a 600 pos/s, cada listado retiene una conexión del pool durante 40 a 100 consultas: los pools
de las cuatro instancias estaban llenos, con 7 a 10 pedidos esperando conexión.

### Las cuatro reglas a la vez

20/11, con [todas_las_reglas.sh](../performance/prueba-openshift/todas_las_reglas.sh): 600 pos/s y 5 minutos de k6 con
la flota en regla; después, una parte de la flota en cada infracción y otros 5 minutos de k6 con el escenario móvil
(un fin de viaje por segundo):

- **Sin habilitación:** ITV vencida a 1.800 vehículos.
- **Viaje sin guía:** 1.800 vehículos sin viaje en curso ni asignado, que siguen mandando posiciones.
- **Desvío de ruta:** 1.800 Guías corridas un grado al norte (unos 111 km).
- **Exceso de carga:** pesajes de 99.000 kg en `balanzas-sur` para los 300 vehículos que termina el escenario móvil.

Los casos de las tres reglas de posición se abren en menos de un minuto (1.800 de cada una) y los de exceso de carga al
terminar cada viaje (249 a 270: los vehículos sin viaje no tienen viaje que terminar). La ingesta sigue en 600 pos/s.
La corrida se hizo antes y después de desplegar #145, y una tercera vez con PostgreSQL en 3 GiB:

| Corrida | Fase | CPU PostgreSQL | Frontoffice p95 (funcionario / responsable / público) | `casos` / `infracciones` / `viajes` p95 | Errores k6 |
| --- | --- | --- | --- | --- | --- |
| Sin #145 | Flota en regla | 3,4 | 1.320 / 1.260 / 403 ms | 1.711 / 1.509 / 1.542 ms | 0 % |
| Sin #145 | Cuatro reglas y móvil | 3,1 | 1.120 / 1.080 / 309 ms | 1.457 / 1.306 / 1.395 ms | 0 % |
| Con #145 | Flota en regla | 2,4 | **204 / 278 / 205 ms** | **239 / 244 / 336 ms** | **0 %** |
| Con #145 | Cuatro reglas y móvil | 2,5 | 575 / 826 / 470 ms | 845 / 730 / 1.223 ms | 0,21 % (33) |
| Con #145, PostgreSQL en 3 GiB | Flota en regla | 2,9 | 362 / 567 / 368 ms | 482 / 501 / 764 ms | 0 % |
| Con #145, PostgreSQL en 3 GiB | Cuatro reglas y móvil | 2,3 | 490 / 852 / 438 ms | 671 / 656 / 1.078 ms | 0,44 % (68) |

En operación normal, con #145, el frontoffice cumple el p95 < 500 ms con margen. En el extremo de esta prueba (5.400
infracciones abiertas de golpe, el 30 % de la flota, y un fin de viaje por segundo) el responsable llega a unos 830 ms,
sobre todo por el listado de viajes de la empresa. La tercera corrida arrancó con la base recién reiniciada (el cambio
de memoria reinicia PostgreSQL), con la caché vacía: por eso su primera fase da más alto que la segunda.

### Procesos de PostgreSQL terminados en el Sandbox

Todos los errores de esas corridas son del mismo segundo: el sistema terminó un proceso de PostgreSQL (`signal 9`), y
PostgreSQL, como hace siempre que muere un proceso, cortó todas las conexiones, se reinicializó en menos de un segundo y
arrancó con su caché (`shared_buffers`) vacía. En el log hay 14 desde el 19/11, casi todos durante las pruebas. Una, el
19/11, reinició el contenedor entero.

No es el límite del pod: con 3 GiB el pico de memoria fue de 1,67 GB y el contador de su cgroup no registra ningún
proceso terminado por memoria (`oom_kill 0`). El pod es de clase Burstable (pide 512 MiB) y el kernel le da prioridad
para terminarlo (`oom_score_adj` 984) cuando el nodo compartido del Sandbox se queda sin memoria. Para que sea
Guaranteed tendría que pedir lo mismo que su límite, también en CPU (4), y la cuota del proyecto permite pedir 3 CPU en
total. En un despliegue propio, PostgreSQL iría con sus recursos reservados (Guaranteed) o como servicio gestionado.

### Consultas de detalle y móvil con eventos acumulados

20/11, con [consultas_detalle.sh](../performance/prueba-openshift/consultas_detalle.sh) y #145 desplegado: 600 pos/s,
5 minutos de k6 con los escenarios de siempre y después 10 minutos sumando las consultas de detalle de funcionarios y
responsables (`TASA_DETALLE=2`), un chofer por segundo con 20 eventos acumulados y su reenvío (`TASA_ACUMULADO=1`) y un
fin de viaje por segundo (`TASA_MOVIL=1`). Con eso k6 recorre 16 lecturas del frontoffice; quedan afuera las de las
pantallas de alta y edición (Guías, permisos, ITV, choferes, perfil de la empresa) y los catálogos.

| Fase | Funcionario | Responsable | Público | Funcionario, detalle | Responsable, detalle | Errores k6 |
| --- | --- | --- | --- | --- | --- | --- |
| Escenarios de siempre (vaciando la cola del arranque) | 220 ms | 382 ms | 271 ms | | | 0 % |
| Todo junto, 10 minutos | **177 ms** | **263 ms** | **198 ms** | **276 ms** | **213 ms** | **0 %** (45.020 pedidos) |

Durante los 10 minutos la ingesta procesó 600 pos/s con 4 instancias, la cola no pasó de unos 300 mensajes y
PostgreSQL usó unas 2,5 CPU. Por pedido (p95): las posiciones de un viaje 171 ms, sus pesajes 223 ms (dos nodos de
balanzas consultados en paralelo), el detalle de un caso 189 ms y su auditoría 209 ms; el más lento del frontoffice es
el listado de viajes en curso de una empresa (372 ms). El detalle está en
[datos/pedidos.txt](../performance/prueba-openshift/datos/pedidos.txt).

**Eventos acumulados:** los 601 lotes de 20 eventos se aplicaron enteros (4.207 cargas parciales, 4.207 descargas
parciales y 3.606 incidentes en la base) y los 601 reenvíos volvieron como duplicados, sin registrar nada de nuevo.
El envío de 20 eventos tarda 525 ms de p95 y el reenvío, 337: la sincronización es idempotente también con carga.

Dos cosas que aparecieron en un primer intento esa misma noche:

- **El Sandbox apaga los pods después de unas 12 horas**, también en medio de una prueba: a la 01:42 bajó Artemis y
  LDAP a 0 réplicas. Los nodos siguieron generando posiciones pero no las podían publicar, y el intento midió el
  frontoffice sin ingesta (no se usó). Al volver el broker, los nodos publicaron lo retenido (unos 35.000 mensajes) y el
  central lo procesó en unos 5 minutos. El script ahora verifica el broker y que la ingesta esté en el volumen
  objetivo antes de medir.
- **Bajar instancias corta algunos pedidos.** Sin la carga de tracking, el autoescalado bajó de 4 a 3 instancias y de
  3 a 2; en esos dos segundos hubo 19 errores (404 y 503, el 0,04 % de los pedidos). WildFly empieza a dar de baja la
  aplicación mientras el router todavía le manda pedidos. Una pausa antes de apagar (`preStop`) lo evitaría.

### Lo que esta prueba no cubre

- **Casos de exceso de carga y de desvío en las corridas 1 a 6:** los nodos de balanzas no tienen pesajes de los
  vehículos `SIM` y el nodo desvía a los vehículos recién después de una hora de recorrido. Se probaron aparte, con
  pesajes cargados en `balanzas-sur` y Guías corridas ([las cuatro reglas a la vez](#las-cuatro-reglas-a-la-vez)).
- **Los reportes de las autoridades (backoffice):** el tablero y la exportación agregan meses de historia; son JSF con
  ingreso por LDAP y no están en k6.
- **Escrituras del frontoffice:** alta de Guías y asignación de viajes, vehículos, permisos e ITV, descargos y
  resoluciones. Su volumen es bajo frente a la ingesta.
- **Un pico con posiciones atrasadas y duplicadas:** el pico de la nube triplicó la frecuencia de forma pareja; el modo
  `NODO_SIN_COBERTURA` del nodo, más fiel al caso real, no se usó en OpenShift.
- **Caídas planificadas de componentes internos** (Artemis, MongoDB, una instancia del central) con carga. Se vieron
  sin planificarlas: PostgreSQL terminado por el Sandbox, el broker apagado por el Sandbox y el autoescalado bajando
  instancias.
- **Una sola corrida por configuración** en general, con k6 desde la máquina de desarrollo: los p95 incluyen la red y
  TLS hasta el clúster (el pedido más rápido tarda unos 140 ms).

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

Cada consumidor de la cola de tracking ocupa una conexión mientras procesa una posición, desde que
empieza la transacción hasta el commit. La concurrencia real de la ingesta es la menor entre
`TRACKING_CONSUMIDORES` (las sesiones abiertas contra la cola) y el pool de instancias del MDB
(`mdb-strict-max-pool`, 4 por procesador que ve la JVM). En OpenShift, con el límite de 2 CPU, ese pool
es de 8: cada instancia procesa como mucho 8 posiciones a la vez y las demás sesiones esperan con un
mensaje tomado. La ingesta ocupa entonces hasta 8 de las 20 conexiones y quedan 12 para la API REST y el
backoffice. Si se sube el límite de CPU, el pool de MDB crece y el tope pasa a ser
`TRACKING_CONSUMIDORES`, que tiene que seguir por debajo de `DB_POOL_MAX`.

### Límites de cada instancia

Leídos el 18/11/2026 en un pod del central en OpenShift (límite de 2 CPU y 2 GiB):

| Qué | Por instancia | Con 4 instancias | De dónde sale |
| --- | --- | --- | --- |
| Pedidos HTTP atendidos a la vez (Undertow) | 32 | 128 | `task-max-threads` del worker `default`: 16 por procesador |
| Conexiones a PostgreSQL | 20 | 80 | `DB_POOL_MAX` |
| Posiciones procesándose a la vez | 8 | 32 | `mdb-strict-max-pool`: 4 por procesador |
| Consumidores de `queue_tracking` en Artemis | 15 | 60 | `TRACKING_CONSUMIDORES` |
| Hilos de las tareas programadas (`@Schedule`) | 10 | 40 | Pool `default` del subsistema `ejb3` |
| Conexiones a MongoDB | hasta 100 | hasta 400 | Valor por defecto del driver (`maxPoolSize` en `MONGO_URI`) |

Hay más pedidos HTTP a la vez (32) que conexiones (20): con carga de la API sola, el pool se puede
agotar antes que los hilos, y eso se ve en `datasources_pool_wait_count`. MongoDB no pasa por WildFly:
el cliente del driver (`AlmacenPosiciones`, uno por instancia) tiene su propio pool, que no aparece en
las estadísticas del datasource; las conexiones abiertas se ven del lado de MongoDB, con
`db.serverStatus().connections`.

Para leer los valores de un pod:

```sh
oc exec deployment/carga-uy -- /opt/jboss/wildfly/bin/jboss-cli.sh -c --commands='/subsystem=io/worker=default:read-attribute(name=task-max-threads),/subsystem=ejb3/strict-max-bean-instance-pool=mdb-strict-max-pool:read-attribute(name=derived-size),/subsystem=ejb3/thread-pool=default:read-attribute(name=max-threads)'
```

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

### El pool de cada instancia en la prueba

En todas las corridas con carga, cada instancia tuvo su pool lleno (`datasources_pool_in_use_count` en 20) y
`datasources_pool_wait_count` subió en proporción a lo procesado: unas 2,3 esperas por posición, por ejemplo unas
1.450 por segundo con una instancia a 610 pos/s y unas 3.600 con cuatro a 1.600 pos/s.

El 19/11 se midió si eso limita ([prueba-openshift/pool.sh](../performance/prueba-openshift/pool.sh)): una sola
instancia, la cola con más posiciones de las que puede procesar y `DB_POOL_MAX` en 20 y en 40. La espera de cada
pedido que espera sale de `datasources_pool_total_blocking_time` sobre `datasources_pool_wait_count`.

| Pool | Procesadas (pos/s) | Procesamiento p95 | Espera por conexión | Hilos esperando (promedio) | Conexiones en uso | CPU de la instancia |
| --- | --- | --- | --- | --- | --- | --- |
| 20 (pod que venía de la corrida anterior) | ~510 | ~28 ms | ~4,5 ms | ~3 | 20 | 1,95 de 2 |
| 40 (pod nuevo) | ~585 | ~14 ms | sin esperas | 0 | 20 a 24 | 1,93 de 2 |
| 20 otra vez (pod nuevo, en el mismo nodo) | ~570 | ~15 ms | ~1,4 ms | ~2 | 20 | 1,95 de 2 |

La primera comparación (510 contra 585) parecía darle la razón al pool, pero al volver a 20 en un pod nuevo la
instancia procesó casi lo mismo que con 40: la diferencia venía del pod, no del pool. Con 20 conexiones las esperas son
de 1 a 5 ms y la instancia está igual en su límite de CPU; con 40 desaparecen, y el uso máximo (24) muestra que a la
instancia le faltan unas pocas conexiones en los picos. **El pool no es el cuello de botella**: lo es la CPU de cada
instancia y, con cuatro, la de PostgreSQL. Tampoco conviene subirlo: con 40 por instancia, 4 instancias pedirían 160
conexiones y PostgreSQL acepta 100. Queda en 20.

### Si las conexiones son el cuello de botella

1. Ajustar `DB_POOL_MAX` y `TRACKING_CONSUMIDORES` según lo medido, siempre con instancias × pool por
   debajo del límite.
2. Poner un pooler de conexiones (PgBouncer, en modo transacción) entre las instancias y PostgreSQL: las
   instancias abren muchas conexiones contra el pooler y él las multiplexa sobre pocas conexiones reales,
   así que la cantidad de instancias deja de depender de `max_connections`. En modo transacción hay que
   desactivar las sentencias preparadas del lado del servidor en el driver (`prepareThreshold=0`).
3. Subir `max_connections` con moderación: cada conexión de PostgreSQL es un proceso con su memoria, y
   muchas conexiones activas compitiendo por pocas CPU rinden peor que menos conexiones bien usadas.
