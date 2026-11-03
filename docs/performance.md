# Performance

[Proyecto](../README.md) · [Observabilidad](observabilidad.md) · [Mensajería](mensajeria.md)

## Volumen objetivo de tracking

El informe ([Vista de Atributos de Calidad](informe/vista-atributos-calidad.tex), AC08) fija el volumen:

| | Valor | De dónde sale |
| --- | --- | --- |
| Parque profesional | 17.869 vehículos con motor (2019) | Registro del MTOP, en el cuadro 2.1 del informe del BID (2022) |
| Tiempo de trabajo | 2.050 h por año, el 23 % del tiempo | Camión pesado de referencia del MTOP, en el informe del INALOG (2021) |
| Hora pico | 10 % a 16 % del tránsito diario | Factor K de la trigésima hora, tabla 385 de PennDOT (2021) |
| Vehículos en viaje en la hora pico | 10.000 a 16.000 | 17.869 × 23 % × (2,4 a 3,8) |
| Vehículos reportando a la vez | **15.000** | Dentro del rango anterior |
| Frecuencia de reporte | una posición cada 30 s | Requisito de carga.uy: ~670 m entre posiciones a 80 km/h |
| Volumen objetivo | **500 posiciones/s** | 15.000 / 30 |
| Pico (×3) | **1.500 posiciones/s** | Vehículos que recuperan cobertura y descargan lo acumulado |

La justificación completa, con lo que deja la estimación del lado alto, está en la sección "Volumen
objetivo de tracking" del informe.

## Cómo generar la carga

El nodo de tracking suma vehículos simulados (`SIM0001`, `SIM0002`…) con rutas al azar.
`NODO_VEHICULOS_EXTRA=N` con `NODO_INTERVALO_SEG=S` genera unas `N / S` posiciones por segundo.
El intervalo por defecto del compose es de 10 s, para que en desarrollo y en la demo el recorrido
de la flota de prueba se vea rápido. La prueba de performance usa el volumen del informe: 15.000
vehículos cada 30 s, repartidos entre los dos nodos. Desde `carga-uy`:

    NODO_INTERVALO_SEG=30 NODO_VEHICULOS_EXTRA=7500 NODO_2_VEHICULOS_EXTRA=7500 \
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
