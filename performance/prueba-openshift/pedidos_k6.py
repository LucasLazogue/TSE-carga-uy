# Tiempo de cada pedido del frontoffice a partir del detalle de k6 (--out csv=...csv.gz): mediana, p95 y p99 por
# nombre de pedido (la etiqueta name de frontoffice.js), del mas lento al mas rapido. El resumen de k6 solo los separa
# por escenario (responsable, funcionario, publico); con esto se ve que pedido se lleva la cola.
#
#   python3 pedidos_k6.py k6-control-detalle.csv.gz [otro.csv.gz ...]
import csv
import gzip
import sys
from collections import defaultdict


def percentil(valores, p):
    valores = sorted(valores)
    return valores[min(len(valores) - 1, int(p * len(valores)))]


for archivo in sys.argv[1:]:
    tiempos = defaultdict(list)
    with gzip.open(archivo, 'rt') as f:
        for fila in csv.DictReader(f):
            if fila['metric_name'] == 'http_req_duration':
                tiempos[fila.get('name') or '?'].append(float(fila['metric_value']))
    print('==', archivo.split('/')[-1])
    for nombre in sorted(tiempos, key=lambda n: -percentil(tiempos[n], .95)):
        t = tiempos[nombre]
        print(f'  {nombre:14s} n={len(t):6d} p50={percentil(t, .5):6.0f} p95={percentil(t, .95):6.0f} p99={percentil(t, .99):6.0f}')
