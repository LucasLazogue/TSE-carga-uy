#!/usr/bin/env bash
AQUI=$(cd "$(dirname "$0")" && pwd); PERF=$(dirname "$AQUI"); REPO=$(dirname "$PERF")
# 600 pos/s + frontoffice + 1 fin de viaje/s durante 12 min; balanzas-norte colgada de +3 a +7 min
L=${RESULTADOS:?definir RESULTADOS, la carpeta de los resultados}
: "${KUBECONFIG:?definir KUBECONFIG, con oc logueado en el proyecto del central}"
NORTE_OK=https://cargauy-balanzas-norte-lucaslaz-dev.apps.rm1.0a51.p1.openshiftapps.com/nodo-balanzas
NORTE_CAIDA=https://10.255.255.1/nodo-balanzas
log() { echo "$1 $(date +%H:%M:%S)" | tee -a $L/bitacora.txt; }
nodos() { KUBECONFIG="$KUBECONFIG_NODOS" oc set env deployment/nodo-tracking-1 deployment/nodo-tracking-2 -n andres-murara-dev "$@" 2>&1 | grep -v memcache; }
psql_c() { oc exec -i $(oc get pods -l deploymentconfig=postgres -o name | head -1) -- sh -c 'psql -U "$POSTGRESQL_USER" -d "$POSTGRESQL_DATABASE" -At'; }
norte() { echo "UPDATE nodoperiferico SET puntoacceso='$1' WHERE identificador='balanzas-norte';" | psql_c; }
trap 'norte $NORTE_OK' EXIT
kill $(cat $L/muestreo.pid) 2>/dev/null
python3 -I "$AQUI"/muestreo.py $L/movil.csv > $L/muestreo-movil.out 2>&1 & echo $! > $L/muestreo.pid
# viajes terminados, pendientes de evaluar la carga y casos de exceso, cada 30 s
( while true; do echo "$(date +%H:%M:%S),$(echo "SELECT (SELECT count(*) FROM viaje WHERE id >= 10000000 AND estado='FINALIZADO' AND fechafin > now() - interval '1 hour'), (SELECT count(*) FROM viaje WHERE proximaevaluacioncarga IS NOT NULL), (SELECT count(*) FROM pesaje);" | psql_c | tr '|' ',')" >> $L/movil-pendientes.csv; sleep 30; done ) & PEND=$!
log "movil+balanza: 600/s inicio"
nodos NODO_VEHICULOS_EXTRA=9000 NODO_INTERVALO_SEG=30
sleep 120
log "movil+balanza: k6 TASA=5 TASA_MOVIL=1 12m inicio"
( cd "$PERF" && JWT_SECRETO="$(oc get secret carga-uy-secretos -o jsonpath='{.data.CARGAUY_JWT_SECRET}' | base64 -d)" TASA=5 TASA_MOVIL=1 MOVIL_DESDE=101 DURACION=12m k6 run --quiet --summary-export=$L/k6-movil.json --out csv=$L/k6-movil-detalle.csv.gz frontoffice.js > $L/k6-movil.txt 2>&1; echo "k6 exit=$?" >> $L/movil.out ) &
K6=$!
sleep 180
log "movil+balanza: norte CAIDA"; norte $NORTE_CAIDA
sleep 240
log "movil+balanza: norte restaurada"; norte $NORTE_OK
wait $K6
log "movil+balanza: k6 fin"
nodos NODO_VEHICULOS_EXTRA=0
log "movil+balanza: carga cortada"
sleep 180
kill $PEND
log "movil+balanza: fin"
