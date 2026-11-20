#!/usr/bin/env bash
# Consultas de detalle del frontoffice y sincronizacion del movil con eventos acumulados, con 600 pos/s. Primero 5
# minutos de k6 con los escenarios de siempre, para calentar la base; despues 10 minutos sumando:
#   - TASA_DETALLE=2: funcionarios que abren el monitoreo de un viaje (posiciones, y pesajes que se les piden en vivo a
#     los nodos de balanzas) y el detalle de un caso con su auditoria; responsables que abren una infraccion; las
#     notificaciones de los dos;
#   - TASA_ACUMULADO=1: un chofer por segundo manda 20 eventos acumulados sin conexion y despues los reenvia;
#   - TASA_MOVIL=1: el fin de un viaje por segundo, como en la corrida 6.
#
# Necesita la base en el perfil performance (carga-uy/openshift/reiniciar-datos.sh --perfil performance), los nodos
# de balanzas encendidos y las variables RESULTADOS, KUBECONFIG (proyecto del central) y KUBECONFIG_NODOS (nodos de
# tracking).
AQUI=$(cd "$(dirname "$0")" && pwd); PERF=$(dirname "$AQUI"); REPO=$(dirname "$PERF")
L=${RESULTADOS:?definir RESULTADOS, la carpeta de los resultados}
: "${KUBECONFIG:?definir KUBECONFIG, con oc logueado en el proyecto del central}"
: "${KUBECONFIG_NODOS:?definir KUBECONFIG_NODOS, con oc logueado en el proyecto de los nodos de tracking}"
log() { echo "$1 $(date +%H:%M:%S)" | tee -a "$L/bitacora.txt"; }
nodos() { KUBECONFIG="$KUBECONFIG_NODOS" oc set env -n "${PROYECTO_NODOS:-andres-murara-dev}" deployment/nodo-tracking-1 deployment/nodo-tracking-2 "$@" 2>&1 | grep -v memcache; }
sql() { oc exec -i "$(oc get pods -l deploymentconfig=postgres -o name | head -1)" -- sh -c 'psql -U "$POSTGRESQL_USER" -d "$POSTGRESQL_DATABASE" -At'; }
k6_corrida() {
    ( cd "$PERF" && JWT_SECRETO="$(oc get secret carga-uy-secretos -o jsonpath='{.data.CARGAUY_JWT_SECRET}' | base64 -d)" \
        TASA=5 env "${@:2}" k6 run --quiet --summary-export="$L/k6-$1.json" --out csv="$L/k6-$1-detalle.csv.gz" \
        frontoffice.js > "$L/k6-$1.txt" 2>&1
      echo "k6 $1 exit=$?" >> "$L/bitacora.txt" )
    # setup_data tiene los tokens firmados para los usuarios de prueba: no se guardan
    python3 -I -c 'import json,sys; d=json.load(open(sys.argv[1])); d.pop("setup_data",None); json.dump(d,open(sys.argv[1],"w"),indent=4)' "$L/k6-$1.json"
}
restaurar() {
    nodos NODO_VEHICULOS_EXTRA=0 NODO_INTERVALO_SEG=30
    kill "$MUESTREO" 2>/dev/null
    log "consultas de detalle: nodos sin carga extra"
}

(cd "$REPO" && bash performance/usuarios.sh 2>&1 | tail -1)
python3 -I "$AQUI"/muestreo.py "$L/consultas-detalle.csv" > "$L/muestreo-consultas-detalle.out" 2>&1 &
MUESTREO=$!
trap restaurar EXIT

# el Sandbox apaga los pods despues de unas 12 horas: sin el broker los nodos generan pero no publican
for d in artemis mongo; do
    [ "$(oc get deployment/$d -o jsonpath='{.status.readyReplicas}')" = 1 ] || { log "consultas de detalle: $d apagado, no se corre"; exit 1; }
done
log "consultas de detalle 600/s inicio"
nodos NODO_VEHICULOS_EXTRA=9000 NODO_INTERVALO_SEG=30
# el autoescalado llega a 4 instancias y se vacia la cola del arranque
sleep 180
# la ingesta tiene que estar en el volumen objetivo antes de medir el frontoffice (columna procesadas_s del muestreo)
procesadas=$(tail -1 "$L/consultas-detalle.csv" | cut -d, -f5)
[ "${procesadas%.*}" -ge 500 ] 2>/dev/null || { log "consultas de detalle: la ingesta procesa ${procesadas:-0} pos/s, no se corre"; exit 1; }
log "consultas de detalle k6 5m inicio (escenarios de siempre, calentamiento)"
k6_corrida detalle-calentamiento DURACION=5m
log "consultas de detalle k6 10m inicio (detalle, movil y eventos acumulados)"
k6_corrida detalle DURACION=10m TASA_DETALLE=2 TASA_ACUMULADO=1 TASA_MOVIL=1
log "consultas de detalle fin"
echo "SELECT tipo, resultado, count(*) FROM eventoviaje WHERE timestamprecepcion >= now() - interval '20 minutes' GROUP BY 1, 2 ORDER BY 1, 2;" | sql | tee -a "$L/bitacora.txt"
