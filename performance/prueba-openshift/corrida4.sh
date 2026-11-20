#!/usr/bin/env bash
AQUI=$(cd "$(dirname "$0")" && pwd); PERF=$(dirname "$AQUI"); REPO=$(dirname "$PERF")
L=${RESULTADOS:?definir RESULTADOS, la carpeta de los resultados}
: "${KUBECONFIG:?definir KUBECONFIG, con oc logueado en el proyecto del central}"
log() { echo "$1 $(date +%H:%M:%S)" | tee -a $L/bitacora.txt; }
nodos() { KUBECONFIG="$KUBECONFIG_NODOS" oc set env deployment/nodo-tracking-1 deployment/nodo-tracking-2 -n andres-murara-dev NODO_VEHICULOS_EXTRA=$1 2>&1 | grep -v memcache; }
k6run() { ( cd "$PERF" && JWT_SECRETO="$(oc get secret carga-uy-secretos -o jsonpath='{.data.CARGAUY_JWT_SECRET}' | base64 -d)" TASA=5 DURACION=$1 k6 run --quiet --summary-export=$L/$2.json frontoffice.js > $L/$2.txt 2>&1 ); echo "k6 $2 exit=$?"; }
python3 -I "$AQUI"/muestreo.py $L/corrida4.csv > $L/muestreo4.out 2>&1 & echo $! > $L/muestreo.pid
log "corrida4 (cache, pg 4cpu) 600/s inicio"
nodos 9000
sleep 180
log "corrida4 600/s + k6 TASA=5 5m inicio"
k6run 5m k6-600-pg4
log "corrida4 600/s + k6 fin"
sleep 60
log "corrida4 900/s inicio"
nodos 13500
sleep 300
log "corrida4 900/s fin"
nodos 0
log "corrida4 carga cortada"
