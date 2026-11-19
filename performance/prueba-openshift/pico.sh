#!/usr/bin/env bash
AQUI=$(cd "$(dirname "$0")" && pwd); PERF=$(dirname "$AQUI"); REPO=$(dirname "$PERF")
L=${RESULTADOS:?definir RESULTADOS, la carpeta de los resultados}
log() { echo "$1 $(date +%H:%M:%S)" | tee -a $L/bitacora.txt; }
nodos() { KUBECONFIG="$KUBECONFIG_NODOS" oc set env deployment/nodo-tracking-1 deployment/nodo-tracking-2 -n andres-murara-dev "$@" 2>&1 | grep -v memcache; }
log "pico 1800/s (intervalo 10 s, flota 600/s) inicio"
nodos NODO_VEHICULOS_EXTRA=9000 NODO_INTERVALO_SEG=10
sleep 300
log "pico fin, vuelve a 600/s"
nodos NODO_INTERVALO_SEG=30
sleep 600
log "pico: fin de la observacion"
nodos NODO_VEHICULOS_EXTRA=0
log "pico carga cortada"
