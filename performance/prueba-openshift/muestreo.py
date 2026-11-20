# muestrea cada 15 s la prueba de performance y agrega una fila al CSV que se le pasa. Necesita KUBECONFIG con oc
# logueado en el proyecto del central. Uso: python3 -I muestreo.py salida.csv
import json, subprocess, time, datetime, os, sys, urllib.parse
KC = os.environ['KUBECONFIG']
SAL = sys.argv[1]
Q = {
 'publicadas_s': 'sum(rate(nodo_eventos_publicados_total[1m]))',
 'generadas_s': 'sum(rate(nodo_eventos_generados_total[1m]))',
 'procesadas_s': 'sum(rate(ingesta_eventos_total[1m]))',
 'cola': 'max(cola_mensajes{cola="queue_tracking"})',
 'consumidores': 'max(cola_consumidores)',
 'proc_p95_ms': 'histogram_quantile(0.95, sum by (le) (rate(ingesta_procesamiento_milliseconds_bucket[1m])))',
 'espera_p95_ms': 'histogram_quantile(0.95, sum by (le) (rate(ingesta_espera_milliseconds_bucket[1m])))',
 'antig_p95_ms': 'histogram_quantile(0.95, sum by (le) (rate(ingesta_antiguedad_milliseconds_bucket[1m])))',
 'pool_uso_max': 'max(datasources_pool_in_use_count{name="CargaUyDS"})',
 'pool_uso_sum': 'sum(datasources_pool_in_use_count{name="CargaUyDS"})',
 'pool_esperas_1m': 'sum(increase(datasources_pool_wait_count{name="CargaUyDS"}[1m]))',
 'errores_1m': 'sum(increase(ingesta_errores_total[1m]))',
 # cuanto esperan por una conexion los que esperan (ms) y cuantos hilos estan esperando en promedio (s/s)
 'pool_espera_ms': 'sum(increase(datasources_pool_total_blocking_time{name="CargaUyDS"}[1m])) / sum(increase(datasources_pool_wait_count{name="CargaUyDS"}[1m]))',
 'pool_esperando': 'sum(rate(datasources_pool_total_blocking_time{name="CargaUyDS"}[1m])) / 1000',
 'pool_fallas_1m': 'sum(increase(datasources_pool_blocking_failure_count{name="CargaUyDS"}[1m]))',
 'pendientes_nodo': 'sum(nodo_eventos_pendientes)',
}
def oc(*a):
    return subprocess.run(['oc', *a], capture_output=True, text=True, env={**os.environ, 'KUBECONFIG': KC}, timeout=60).stdout
obs = oc('get', 'pods', '-o', 'name').split()
obs = [p for p in obs if 'observabilidad' in p][0]
cab = ['hora', 'pods_central'] + list(Q) + ['cpu_central_m', 'cpu_postgres_m', 'cpu_artemis_m', 'cpu_mongo_m', 'casos']
pg = [p for p in oc('get', 'pods', '-l', 'deploymentconfig=postgres', '-o', 'name').split()][0]
def casos():
    sql = 'SELECT count(*) FROM casofiscalizacion;'
    r = subprocess.run(['oc', 'exec', '-i', pg, '--', 'sh', '-c', 'psql -U "$POSTGRESQL_USER" -d "$POSTGRESQL_DATABASE" -At'],
                       input=sql, capture_output=True, text=True, env={**os.environ, 'KUBECONFIG': KC}, timeout=60)
    return r.stdout.strip()
if not os.path.exists(SAL):
    open(SAL, 'w').write(','.join(cab) + '\n')
while True:
    try:
        script = ';'.join("curl -s --data-urlencode %s localhost:9090/api/v1/query; echo" % ("'query=" + q.replace("'", "'\\''") + "'") for q in Q.values())
        salida = oc('exec', obs, '--', 'sh', '-c', script).strip().split('\n')
        vals = []
        for linea in salida:
            try:
                r = json.loads(linea)['data']['result']
                v = float(r[0]['value'][1]) if r else ''
                vals.append('' if v == '' or v != v else round(v, 1))
            except Exception:
                vals.append('')
        pods = oc('get', 'hpa', 'carga-uy', '-o', 'jsonpath={.status.currentReplicas}')
        top = oc('adm', 'top', 'pods', '--no-headers')
        cpu = {'carga-uy': 0, 'postgres': 0, 'artemis': 0, 'mongo': 0}
        for l in top.splitlines():
            n, c = l.split()[0], l.split()[1]
            m = int(c.rstrip('m')) if c.endswith('m') else int(float(c) * 1000)
            for k in cpu:
                if n.startswith(k + '-'):
                    cpu[k] += m
        fila = [datetime.datetime.now().strftime('%H:%M:%S'), pods] + vals + [cpu[k] for k in cpu] + [casos()]
        open(SAL, 'a').write(','.join(str(x) for x in fila) + '\n')
    except Exception as e:
        open(SAL + '.err', 'a').write(f'{datetime.datetime.now()} {e}\n')
    time.sleep(15)
