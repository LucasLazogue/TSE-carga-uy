#!/usr/bin/env bash
AQUI=$(cd "$(dirname "$0")" && pwd); PERF=$(dirname "$AQUI"); REPO=$(dirname "$PERF")
# capacidad de ingesta con 1, 2 y 4 instancias fijas: se llena la cola y se mide a que ritmo la vacia cada configuracion
L=${RESULTADOS:?definir RESULTADOS, la carpeta de los resultados}
: "${KUBECONFIG:?definir KUBECONFIG, con oc logueado en el proyecto del central}"
log() { echo "$1 $(date +%H:%M:%S)" | tee -a $L/bitacora.txt; }
nodos() { KUBECONFIG="$KUBECONFIG_NODOS" oc set env deployment/nodo-tracking-1 deployment/nodo-tracking-2 -n andres-murara-dev "$@" 2>&1 | grep -v memcache; }
instancias() { oc patch hpa carga-uy --type=merge -p "{\"spec\":{\"minReplicas\":$1,\"maxReplicas\":$1}}" >/dev/null; 
  until [ "$(oc get deploy carga-uy -o jsonpath='{.status.readyReplicas}')" = "$1" ] && [ "$(oc get deploy carga-uy -o jsonpath='{.status.replicas}')" = "$1" ]; do sleep 5; done; log "escalado $1 instancias listas"; }
log "escalado3: 4 instancias fijas, carga 2700/s 4 min inicio"
instancias 4
nodos NODO_VEHICULOS_EXTRA=13500 NODO_INTERVALO_SEG=10
sleep 240
nodos NODO_VEHICULOS_EXTRA=0 NODO_INTERVALO_SEG=30
log "escalado3: nodos apagados, vaciado con 4 instancias"
sleep 200
log "escalado3: fin"
