#!/usr/bin/env bash
# Control de los casos abiertos despues de #141 (el motor ya no recarga el caso en cada posicion). Repite los controles
# de la tarde del 19/11 (control.csv y control2.csv) con la version nueva: 600 pos/s y k6 5 minutos con la mitad de la
# flota con caso abierto, y despues otros 5 minutos con las ITV vigentes de nuevo.
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
k6_5m() {
    ( cd "$PERF" && JWT_SECRETO="$(oc get secret carga-uy-secretos -o jsonpath='{.data.CARGAUY_JWT_SECRET}' | base64 -d)" \
        TASA=5 DURACION=5m k6 run --quiet --summary-export="$L/k6-$1.json" frontoffice.js > "$L/k6-$1.txt" 2>&1
      echo "k6 $1 exit=$?" >> "$L/bitacora.txt" )
    # setup_data tiene los tokens firmados para los usuarios de prueba: no se guardan
    python3 -I -c 'import json,sys; d=json.load(open(sys.argv[1])); d.pop("setup_data",None); json.dump(d,open(sys.argv[1],"w"),indent=4)' "$L/k6-$1.json"
}
restaurar() {
    nodos NODO_VEHICULOS_EXTRA=0 NODO_INTERVALO_SEG=30
    kill "$MUESTREO" 2>/dev/null
    log "control casos: nodos sin carga extra"
}

(cd "$REPO" && bash performance/usuarios.sh 2>&1 | tail -1)
python3 -I "$AQUI"/muestreo.py "$L/control-141.csv" > "$L/muestreo-control-141.out" 2>&1 &
MUESTREO=$!
trap restaurar EXIT

log "control casos 600/s inicio"
nodos NODO_VEHICULOS_EXTRA=9000 NODO_INTERVALO_SEG=30
# el autoescalado llega a 4 instancias y se vacia la cola del arranque
sleep 180
# con la base recien cargada la primera fase da tiempos mas altos (caches frias): 5 minutos de k6 que no se comparan
log "control casos k6 5m inicio (calentamiento, ITV vigentes)"
k6_5m control-141-calentamiento
log "control casos: ITV vencida a la mitad de la flota"
echo "UPDATE itv SET fechavencimiento = current_date - 1 WHERE id >= 10000000 AND (id - 10000000) % 10 BETWEEN 0 AND 4;" | sql
# la primera posicion de cada uno abre el caso; se espera a que esten todos y a que la cola se estabilice
until [ "$(echo "SELECT count(*) FROM casofiscalizacion WHERE fechadeteccion >= current_date AND tipo = 'SIN_HABILITACION';" | sql)" -ge 8900 ]; do sleep 10; done
log "control casos: casos abiertos"
sleep 90
log "control casos k6 5m inicio (mitad de la flota con caso abierto)"
k6_5m control-141
log "control casos: ITV vigentes de nuevo"
echo "UPDATE itv SET fechavencimiento = current_date + 365 WHERE id >= 10000000 AND (id - 10000000) % 10 BETWEEN 0 AND 4;" | sql
sleep 30
log "control casos k6 5m inicio (ITV vigentes)"
k6_5m control-141-vigentes
log "control casos fin"
echo "SELECT tipo, count(*) FROM casofiscalizacion WHERE fechadeteccion >= current_date GROUP BY tipo;" | sql | tee -a "$L/bitacora.txt"
