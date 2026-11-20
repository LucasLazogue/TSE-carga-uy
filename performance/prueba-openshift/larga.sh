#!/usr/bin/env bash
AQUI=$(cd "$(dirname "$0")" && pwd); PERF=$(dirname "$AQUI"); REPO=$(dirname "$PERF")
L=${RESULTADOS:?definir RESULTADOS, la carpeta de los resultados}
: "${KUBECONFIG:?definir KUBECONFIG, con oc logueado en el proyecto del central}"
log() { echo "$1 $(date +%H:%M:%S)" | tee -a $L/bitacora.txt; }
nodos() { KUBECONFIG="$KUBECONFIG_NODOS" oc set env deployment/nodo-tracking-1 deployment/nodo-tracking-2 -n andres-murara-dev "$@" 2>&1 | grep -v memcache; }
kill $(cat $L/muestreo.pid) 2>/dev/null
python3 -I "$AQUI"/muestreo.py $L/larga.csv > $L/muestreo-larga.out 2>&1 & echo $! > $L/muestreo.pid
log "larga 600/s inicio"
nodos NODO_VEHICULOS_EXTRA=9000 NODO_INTERVALO_SEG=30
sleep 120
log "larga k6 TASA=5 40m inicio"
( cd "$PERF" && JWT_SECRETO="$(oc get secret carga-uy-secretos -o jsonpath='{.data.CARGAUY_JWT_SECRET}' | base64 -d)" TASA=5 DURACION=40m k6 run --quiet --summary-export=$L/k6-larga.json --out csv=$L/k6-larga-detalle.csv.gz frontoffice.js > $L/k6-larga.txt 2>&1 ); echo "k6 exit=$?"
log "larga k6 fin"
nodos NODO_VEHICULOS_EXTRA=0
log "larga carga cortada"
