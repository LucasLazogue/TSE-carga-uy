#!/usr/bin/env bash
AQUI=$(cd "$(dirname "$0")" && pwd); PERF=$(dirname "$AQUI"); REPO=$(dirname "$PERF")
# capacidad de ingesta con 1, 2 y 4 instancias fijas: se llena la cola y se mide a que ritmo la vacia cada configuracion
L=${RESULTADOS:?definir RESULTADOS, la carpeta de los resultados}
: "${KUBECONFIG:?definir KUBECONFIG, con oc logueado en el proyecto del central}"
log() { echo "$1 $(date +%H:%M:%S)" | tee -a $L/bitacora.txt; }
nodos() { KUBECONFIG="$KUBECONFIG_NODOS" oc set env deployment/nodo-tracking-1 deployment/nodo-tracking-2 -n andres-murara-dev "$@" 2>&1 | grep -v memcache; }
instancias() { oc patch hpa carga-uy --type=merge -p "{\"spec\":{\"minReplicas\":$1,\"maxReplicas\":$1}}" >/dev/null; 
  until [ "$(oc get deploy carga-uy -o jsonpath='{.status.readyReplicas}')" = "$1" ] && [ "$(oc get deploy carga-uy -o jsonpath='{.status.replicas}')" = "$1" ]; do sleep 5; done; log "escalado $1 instancias listas"; }
kill $(cat $L/muestreo.pid) 2>/dev/null
python3 -I "$AQUI"/muestreo.py $L/escalado.csv > $L/muestreo-escalado.out 2>&1 & echo $! > $L/muestreo.pid
log "escalado: mongo a 2 CPU"
oc set resources deployment/mongo --limits=cpu=2,memory=1Gi --requests=cpu=100m,memory=256Mi >/dev/null
oc rollout status deployment/mongo --timeout=300s | tail -1
instancias 1
log "escalado: llenado de cola 1800/s 3.5 min inicio"
nodos NODO_VEHICULOS_EXTRA=9000 NODO_INTERVALO_SEG=10
sleep 210
nodos NODO_VEHICULOS_EXTRA=0 NODO_INTERVALO_SEG=30
log "escalado: nodos apagados, vaciado con 1 instancia"
sleep 200
log "escalado: paso a 2"
instancias 2
sleep 180
log "escalado: paso a 4"
instancias 4
sleep 180
log "escalado: fin de la medicion"
