#!/usr/bin/env bash
# Rafaga de casos de fiscalizacion: con 600 pos/s y k6 estables, se vence la ITV del 10 % de la flota y despues la de
# otro 40 %. La primera posicion de cada uno de esos vehiculos abre un caso "sin habilitacion" (con sus evidencias y la
# auditoria), asi que en unos 30 s se abren 1.800 casos y despues 7.200: lo que pasaria a medianoche, cuando la clave
# de deduplicacion cambia de dia. Mide si abrir casos en masa afecta la ingesta, el pool de conexiones y el frontoffice.
#
# Necesita la base en el perfil performance sin vehiculos irregulares:
#   carga-uy/openshift/reiniciar-datos.sh --perfil performance --si -- -v cada_irregular=0
# y las variables RESULTADOS, KUBECONFIG (proyecto del central) y KUBECONFIG_NODOS (proyecto de los nodos de tracking).
AQUI=$(cd "$(dirname "$0")" && pwd); PERF=$(dirname "$AQUI"); REPO=$(dirname "$PERF")
L=${RESULTADOS:?definir RESULTADOS, la carpeta de los resultados}
: "${KUBECONFIG:?definir KUBECONFIG, con oc logueado en el proyecto del central}"
: "${KUBECONFIG_NODOS:?definir KUBECONFIG_NODOS, con oc logueado en el proyecto de los nodos de tracking}"
log() { echo "$1 $(date +%H:%M:%S)" | tee -a "$L/bitacora.txt"; }
nodos() { KUBECONFIG="$KUBECONFIG_NODOS" oc set env -n "${PROYECTO_NODOS:-andres-murara-dev}" deployment/nodo-tracking-1 deployment/nodo-tracking-2 "$@" 2>&1 | grep -v memcache; }
sql() { oc exec -i "$(oc get pods -l deploymentconfig=postgres -o name | head -1)" -- sh -c 'psql -U "$POSTGRESQL_USER" -d "$POSTGRESQL_DATABASE" -At'; }

(cd "$REPO" && bash performance/usuarios.sh 2>&1 | tail -1)
python3 -I "$AQUI"/muestreo.py "$L/rafaga.csv" > "$L/muestreo-rafaga.out" 2>&1 &
MUESTREO=$!
trap 'kill $MUESTREO 2>/dev/null' EXIT

log "rafaga 600/s inicio"
nodos NODO_VEHICULOS_EXTRA=9000 NODO_INTERVALO_SEG=30
# el autoescalado llega a 4 instancias y se vacia la cola del arranque
sleep 180
log "rafaga k6 TASA=5 16m inicio"
( cd "$PERF" && JWT_SECRETO="$(oc get secret carga-uy-secretos -o jsonpath='{.data.CARGAUY_JWT_SECRET}' | base64 -d)" \
    TASA=5 DURACION=16m k6 run --quiet --summary-export="$L/k6-rafaga.json" --out csv="$L/k6-rafaga-detalle.csv.gz" \
    frontoffice.js > "$L/k6-rafaga.txt" 2>&1; echo "k6 exit=$?" >> "$L/bitacora.txt" ) &
K6=$!
sleep 300
log "rafaga 1: ITV vencida al 10 % de la flota"
echo "UPDATE itv SET fechavencimiento = current_date - 1 WHERE id >= 10000000 AND (id - 10000000) % 10 = 0;" | sql
sleep 300
log "rafaga 2: ITV vencida a otro 40 % de la flota"
echo "UPDATE itv SET fechavencimiento = current_date - 1 WHERE id >= 10000000 AND (id - 10000000) % 10 BETWEEN 1 AND 4;" | sql
wait $K6
log "rafaga k6 fin"
echo "SELECT tipo, count(*) FROM casofiscalizacion WHERE fechadeteccion >= current_date GROUP BY tipo;" | sql | tee -a "$L/bitacora.txt"
