#!/usr/bin/env bash
# Pool de conexiones 20 contra 40: una sola instancia del central, la cola con mas posiciones de las que puede procesar
# (900 pos/s contra unas 600 de capacidad) y DB_POOL_MAX en 20 y despues en 40. Si con 40 procesa mas o el
# procesamiento baja, el pool de cada instancia era un limite; si no, el limite es la CPU y las esperas son cortas.
# Al terminar saca DB_POOL_MAX (vuelve al valor por defecto, 20), el autoescalado de 1 a 4 y los nodos sin carga extra.
#
# Necesita las variables RESULTADOS, KUBECONFIG (proyecto del central) y KUBECONFIG_NODOS (nodos de tracking).
AQUI=$(cd "$(dirname "$0")" && pwd)
L=${RESULTADOS:?definir RESULTADOS, la carpeta de los resultados}
: "${KUBECONFIG:?definir KUBECONFIG, con oc logueado en el proyecto del central}"
: "${KUBECONFIG_NODOS:?definir KUBECONFIG_NODOS, con oc logueado en el proyecto de los nodos de tracking}"
log() { echo "$1 $(date +%H:%M:%S)" | tee -a "$L/bitacora.txt"; }
nodos() { KUBECONFIG="$KUBECONFIG_NODOS" oc set env -n "${PROYECTO_NODOS:-andres-murara-dev}" deployment/nodo-tracking-1 deployment/nodo-tracking-2 "$@" 2>&1 | grep -v memcache; }
restaurar() {
    nodos NODO_VEHICULOS_EXTRA=0 NODO_INTERVALO_SEG=30
    oc set env deployment/carga-uy DB_POOL_MAX-
    oc patch hpa carga-uy -p '{"spec":{"minReplicas":1,"maxReplicas":4}}'
    kill "$MUESTREO" 2>/dev/null
    log "pool restaurado: DB_POOL_MAX=20, autoescalado 1-4, nodos sin carga extra"
}

python3 -I "$AQUI"/muestreo.py "$L/pool.csv" > "$L/muestreo-pool.out" 2>&1 &
MUESTREO=$!
trap restaurar EXIT

oc patch hpa carga-uy -p '{"spec":{"minReplicas":1,"maxReplicas":1}}'
log "pool 20: 1 instancia, 900 pos/s"
nodos NODO_VEHICULOS_EXTRA=9000 NODO_INTERVALO_SEG=20
sleep 420
log "pool 40: cambio de DB_POOL_MAX"
oc set env deployment/carga-uy DB_POOL_MAX=40
oc rollout status deployment/carga-uy --timeout=600s
log "pool 40: instancia nueva lista"
sleep 420
log "pool fin"
