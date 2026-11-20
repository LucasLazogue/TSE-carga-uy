#!/usr/bin/env bash
# Las cuatro reglas de fiscalizacion a la vez con 600 pos/s y k6, con el tiempo de cada pedido del frontoffice
# (--out csv). Primero 5 minutos de k6 con la flota en regla; despues, con una parte de la flota en cada infraccion,
# otros 5 minutos con el escenario movil (1 fin de viaje por segundo):
#   - sin habilitacion: ITV vencida a los vehiculos n % 10 = 0 (la primera posicion abre el caso);
#   - viaje sin guia: n % 10 = 1 terminan el viaje en curso y el asignado, y siguen mandando posiciones;
#   - desvio de ruta: n % 10 = 2 con la Guia del viaje en curso corrida un grado al norte (unos 111 km);
#   - exceso de carga: pesajes por encima del maximo en balanzas-sur para los vehiculos que termina el escenario
#     movil (SIM0001 a SIM0300): al terminar el viaje, el central le pide los pesajes a los nodos y abre el caso.
#
# Necesita la base en el perfil performance sin vehiculos irregulares:
#   carga-uy/openshift/reiniciar-datos.sh --perfil performance --si -- -v cada_irregular=0
# los nodos de balanzas encendidos y las variables RESULTADOS, KUBECONFIG (proyecto del central), KUBECONFIG_NODOS
# (nodos de tracking) y CLAVE_BALANZA (la de una balanza de balanzas-sur, para registrar los pesajes).
AQUI=$(cd "$(dirname "$0")" && pwd); PERF=$(dirname "$AQUI"); REPO=$(dirname "$PERF")
L=${RESULTADOS:?definir RESULTADOS, la carpeta de los resultados}
: "${KUBECONFIG:?definir KUBECONFIG, con oc logueado en el proyecto del central}"
: "${KUBECONFIG_NODOS:?definir KUBECONFIG_NODOS, con oc logueado en el proyecto de los nodos de tracking}"
: "${CLAVE_BALANZA:?definir CLAVE_BALANZA, la clave de una balanza de balanzas-sur}"
PESAJES=${PESAJES_URL:-https://balanzas-sur-lucaslaz-dev.apps.rm1.0a51.p1.openshiftapps.com/nodo-balanzas/api/pesajes}
MOVIL=300
log() { echo "$1 $(date +%H:%M:%S)" | tee -a "$L/bitacora.txt"; }
nodos() { KUBECONFIG="$KUBECONFIG_NODOS" oc set env -n "${PROYECTO_NODOS:-andres-murara-dev}" deployment/nodo-tracking-1 deployment/nodo-tracking-2 "$@" 2>&1 | grep -v memcache; }
sql() { oc exec -i "$(oc get pods -l deploymentconfig=postgres -o name | head -1)" -- sh -c 'psql -U "$POSTGRESQL_USER" -d "$POSTGRESQL_DATABASE" -At'; }
casos() { echo "SELECT tipo, count(*) FROM casofiscalizacion WHERE fechadeteccion >= current_date GROUP BY tipo ORDER BY tipo;" | sql; }
k6_5m() {
    ( cd "$PERF" && JWT_SECRETO="$(oc get secret carga-uy-secretos -o jsonpath='{.data.CARGAUY_JWT_SECRET}' | base64 -d)" \
        TASA=5 DURACION=5m env "${@:2}" k6 run --quiet --summary-export="$L/k6-$1.json" --out csv="$L/k6-$1-detalle.csv.gz" \
        frontoffice.js > "$L/k6-$1.txt" 2>&1
      echo "k6 $1 exit=$?" >> "$L/bitacora.txt" )
    # setup_data tiene los tokens firmados para los usuarios de prueba: no se guardan
    python3 -I -c 'import json,sys; d=json.load(open(sys.argv[1])); d.pop("setup_data",None); json.dump(d,open(sys.argv[1],"w"),indent=4)' "$L/k6-$1.json"
}
restaurar() {
    nodos NODO_VEHICULOS_EXTRA=0 NODO_INTERVALO_SEG=30
    kill "$MUESTREO" 2>/dev/null
    log "todas las reglas: nodos sin carga extra"
}

(cd "$REPO" && bash performance/usuarios.sh 2>&1 | tail -1)
python3 -I "$AQUI"/muestreo.py "$L/todas-las-reglas.csv" > "$L/muestreo-todas-las-reglas.out" 2>&1 &
MUESTREO=$!
trap restaurar EXIT

log "todas las reglas 600/s inicio"
nodos NODO_VEHICULOS_EXTRA=9000 NODO_INTERVALO_SEG=30
# el autoescalado llega a 4 instancias y se vacia la cola del arranque
sleep 180
log "todas las reglas k6 5m inicio (flota en regla)"
k6_5m reglas-en-regla

log "todas las reglas: pesajes de exceso en balanzas-sur para SIM0001 a SIM$(printf %04d $MOVIL)"
# 10 minutos antes: dentro del viaje en curso, que empezo entre 1 y 2 horas antes de la carga de datos
python3 -I - "$MOVIL" > "$L/pesajes-exceso.json" <<'EOF'
import datetime, json, sys, zoneinfo
hora = (datetime.datetime.now(zoneinfo.ZoneInfo('America/Montevideo')) - datetime.timedelta(minutes=10)).strftime('%Y-%m-%dT%H:%M:%S')
print(json.dumps([{'idRegistro': f'prf-exceso-{n}', 'matricula': f'SIM{n:04d}', 'fechaHora': hora, 'pesoRegistrado': 99000}
                  for n in range(1, int(sys.argv[1]) + 1)]))
EOF
curl -s -o /dev/null -w "pesajes %{http_code}\n" -X POST "$PESAJES" -H "Authorization: Bearer $CLAVE_BALANZA" \
    -H 'Content-Type: application/json' --data-binary @"$L/pesajes-exceso.json" | tee -a "$L/bitacora.txt"

log "todas las reglas: infracciones en la flota"
sql <<'EOF'
UPDATE itv SET fechavencimiento = current_date - 1 WHERE id >= 10000000 AND (id - 10000000) % 10 = 0;
UPDATE viaje SET estado = 'FINALIZADO',
       fechainicio = coalesce(fechainicio, (now() AT TIME ZONE 'America/Montevideo') - interval '1 minute'),
       fechafin = (now() AT TIME ZONE 'America/Montevideo') - interval '1 minute'
 WHERE id >= 10000000 AND estado IN ('EN_CURSO', 'ASIGNADO') AND (vehiculo_id - 10000000) % 10 = 1;
UPDATE guia SET origenlat = origenlat + 1, destinolat = destinolat + 1
 WHERE id >= 10000000 AND id < 20000000 AND (id - 10000000) % 10 = 2;
EOF
# hasta que esten casi todos los casos de las tres reglas de posicion (1.800 vehiculos cada una), con un tope
for _ in $(seq 60); do
    [ "$(casos | awk -F'|' '$1 != "EXCESO_CARGA" && $2 >= 1700' | wc -l)" -ge 3 ] && break
    sleep 10
done
casos | tee -a "$L/bitacora.txt"
log "todas las reglas: casos de posicion abiertos"
sleep 60
log "todas las reglas k6 5m inicio (cuatro reglas, escenario movil)"
k6_5m reglas-todas TASA_MOVIL=1 MOVIL_DESDE=1
# la evaluacion de carga corre en segundo plano despues de cada fin de viaje
sleep 60
log "todas las reglas fin"
casos | tee -a "$L/bitacora.txt"
