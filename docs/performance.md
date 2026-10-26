# Performance

[Proyecto](../README.md) · [Observabilidad](observabilidad.md) · [Mensajería](mensajeria.md)

## Volumen objetivo de tracking

El informe ([Vista de Atributos de Calidad](informe/vista-atributos-calidad.tex), AC08) fija el volumen:

| | Valor | De dónde sale |
| --- | --- | --- |
| Vehículos reportando a la vez | 15.000 | Un tercio de los ~45.000 vehículos de carga registrados |
| Frecuencia de reporte | una posición cada 30 s | Valor habitual en seguimiento de flotas |
| Volumen objetivo | **500 posiciones/s** | 15.000 / 30 |
| Pico (×3) | **1.500 posiciones/s** | Vehículos que recuperan cobertura y descargan lo acumulado |

## Cómo generar la carga

El nodo de tracking suma vehículos simulados (`SIM0001`, `SIM0002`…) con rutas al azar. Con el
intervalo del compose (10 s), `NODO_VEHICULOS_EXTRA=N` genera unas `N / 10` posiciones por segundo.
Desde `carga-uy`:

    NODO_VEHICULOS_EXTRA=5000 docker compose up -d nodo-tracking

Para el pico, `NODO_SIN_COBERTURA=desde,durante` (en minutos) retiene las posiciones y las publica
todas juntas al volver la cobertura. Para volver a la flota normal, `NODO_VEHICULOS_EXTRA=0`.

Se mira en Grafana, en el tablero **carga.uy · ingesta, nodo y API**:
- posiciones generadas en el nodo frente a procesadas en el central;
- la cola de tracking, que tiene que estabilizarse y no crecer sin límite;
- la espera en la cola y la antigüedad de cada posición al procesarse;
- el tiempo de procesamiento.

Las matrículas simuladas no están registradas: se guardan sin vehículo y no pasan por la
fiscalización, así que la carga real por posición es algo mayor que la medida.

## Prueba preliminar (26/10/2026, máquina de desarrollo)

Todo el compose en una sola Mac de 8 núcleos y 8 GB para Docker: el nodo de tracking que genera la
carga, el broker, el central, las dos bases y los nodos de balanzas. Escalones de unos 2,5 minutos:

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
