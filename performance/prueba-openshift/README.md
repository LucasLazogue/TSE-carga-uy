# Prueba en OpenShift (18 y 19/11/2026)

Scripts con los que se corrió la prueba de performance en OpenShift y los datos que registraron. Los resultados y su
análisis están en [docs/performance.md](../../docs/performance.md#prueba-en-openshift-18-y-19112026); acá queda cómo
se ejecutó cada corrida, para poder repetirla.

## Variables

| Variable | Qué es |
| --- | --- |
| `RESULTADOS` | Carpeta donde cada script deja sus CSV, los resúmenes de k6 y `bitacora.txt` |
| `KUBECONFIG` | Sesión de `oc` en el proyecto del central |
| `KUBECONFIG_NODOS` | Sesión de `oc` en el proyecto de los nodos de tracking (`PROYECTO_NODOS`, por defecto `andres-murara-dev`) |
| `CENTRAL` | URL del central para los scripts de Python (por defecto, la del proyecto de la prueba) |
| `JWT_SECRETO` | Lo leen los scripts del Secret `carga-uy-secretos` cuando lo necesitan; no se escribe en ningún archivo |

Antes de cada corrida, la base en el perfil `performance`
([reiniciar-datos.sh](../../carga-uy/openshift/reiniciar-datos.sh) `--perfil performance`) y `performance/usuarios.sh`
para los usuarios de k6. Al terminar, `reiniciar-datos.sh` sin parámetros vuelve al perfil `demo`.

## Scripts

| Script | Corrida |
| --- | --- |
| `muestreo.py` | Corre en segundo plano en todas: cada 15 s, las métricas de Prometheus (ingesta, cola, pool), la CPU de cada pod y la cantidad de casos, en un CSV |
| `corrida3.sh`, `corrida4.sh` | Corridas 3 y 4: k6 solo, 600 pos/s con k6 y 900 pos/s |
| `pico.sh` | Pico de 1.800 pos/s durante 5 minutos y vuelta a 600 |
| `larga.sh` | Corrida 5: 600 pos/s con k6 durante 40 minutos |
| `escalado.sh`, `escalado2.sh`, `escalado3.sh` | Escalado con la cola llena: `escalado.sh`, 1 y 2 instancias fijas (y arranca el muestreo); `escalado2.sh` intentó 4, pero la cola se vació antes; `escalado3.sh` fija 4 instancias con 2.700 pos/s, la fase que quedó en el informe |
| `movil_balanza.sh` | Corrida 6: 600 pos/s, k6 con el escenario móvil y `balanzas-norte` caída 4 minutos (`balanza_caida.py`) |
| `balanza_caida.py` | Pide los pesajes de viajes mientras un nodo de balanzas apunta a una dirección que no contesta |
| `pdi_caida.py` | PDI que no responde, en el compose local (`GUBUY_MOCK=true`, suplantador pausado con `docker pause`) |
| `rafaga_casos.sh` | Ráfaga de casos: con 600 pos/s y k6 estables, vence la ITV del 10 % y después de otro 40 % de la flota |
| `pool.sh` | Pool de conexiones 20 contra 40 con una instancia y la cola llena; al terminar deja todo como estaba |
| `control_casos.sh` | Control de los casos abiertos con #141 desplegado: 5 minutos de k6 para calentar, la mitad de la flota con caso abierto y las ITV vigentes de nuevo. La repetición de las dos fases, alternadas, se hizo a mano con los mismos comandos (ver la bitácora) |
| `medir_endpoints.py`, `un_pesaje.py` | Auxiliares: tiempo de cada endpoint del frontoffice y un pedido de pesajes suelto |

Las corridas 1 y 2 se hicieron a mano, cambiando `NODO_VEHICULOS_EXTRA` en los nodos (ver la bitácora).

## Datos

`datos/bitacora.txt` tiene la hora de cada paso de las dos jornadas. Los CSV de `muestreo.py`:

| Archivo | Corrida |
| --- | --- |
| `corrida1-sin-indices.csv` | 1: 150 pos/s sin índices, PostgreSQL con el límite por defecto |
| `corrida2.csv` | 2: escalones de 150, 300 y 600 pos/s, 600 con k6 y k6 solo |
| `corrida3.csv` | 3: caché de indicadores, PostgreSQL con 2 CPU |
| `corrida4.csv` | 4: PostgreSQL con 4 CPU, 600 con k6, 900 y el pico |
| `larga.csv` | 5: 40 minutos (sigue hasta el arranque de la 6) |
| `movil.csv`, `movil-pendientes.csv` | 6, y las evaluaciones de carga pendientes durante la caída del nodo |
| `escalado.csv` | Escalado con 1, 2 y 4 instancias |
| `balanza-caida.csv`, `balanza-caida-4.csv` | Pedidos de pesajes con un nodo caído, antes y después de #131 y #134 (`hora,fase,codigo,ms,sin_responder`) |
| `pdi-caida.csv` | Ingresos y consultas públicas con la PDI sin responder |
| `rafaga.csv` | Ráfaga de casos |
| `control.csv`, `control2.csv` | Controles de 5 minutos con 600 pos/s y k6: con la mitad de la flota con caso abierto y con las ITV vigentes |
| `pool.csv`, `pool-20-otra-vez.csv` | Pool de 20 y de 40, y de nuevo 20 en un pod nuevo |
| `control-141.csv`, `control-141-repeticion.csv` | Control de los casos abiertos con #141 desplegado, y la repetición |

Los `k6-*.txt` y `k6-*.json` son los resúmenes de k6 de cada corrida, y `pg-activas.txt`, las consultas que se veían en
`pg_stat_activity` cuando la consulta de indicadores públicos saturaba la base (corrida 2). El detalle de cada pedido de
k6 (decenas de MB) no se versiona, y los `k6-*.json` se guardan sin `setup_data`: ahí k6 deja los tokens que firma
para los usuarios de prueba, que valen 6 horas contra el central.
