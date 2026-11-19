#!/usr/bin/env bash
# Deja los datos del central de OpenShift como recien instalado: borra la base de PostgreSQL y las posiciones de
# MongoDB, y vuelve a encender el central, que crea el esquema y carga los datos del perfil elegido. Si existe el
# Secret carga-uy-demo, carga ademas el elenco de la demo (ElencoDemo): sin los datos de prueba solo quedan los nodos
# y las personas que no dependen de la empresa de la demo. Sirve para repetir la demo o una prueba desde el mismo
# punto. En Compose, ver "Datos del central" en docs/infraestructura.md.
#
# Perfiles:
#   demo         (por defecto) datos de prueba inventados: empresas, vehiculos, viajes, casos e historia
#   base         solo roles, rubros, tipos de carga y normas de fiscalizacion
#   performance  base mas performance/datos.sql: la flota del volumen objetivo, con viajes en curso y meses de historia
#
# El perfil queda en el Deployment (CARGAUY_DATOS) hasta el proximo reinicio: un despliegue no lo cambia.
#
# Uso, con oc logueado en el proyecto del central:
#   ./reiniciar-datos.sh                          perfil demo; pide confirmacion escribiendo el nombre del proyecto
#   ./reiniciar-datos.sh --perfil performance     otro perfil
#   ./reiniciar-datos.sh --si                     sin preguntar (para scripts)
#   ./reiniciar-datos.sh --perfil performance -- -v vehiculos=6000 -v meses=3
#                                                 lo que va despues de -- se le pasa a psql (parametros de datos.sql)
#
# No toca: los usuarios de LDAP (backoffice), las cuentas de gub.uy, la cola de Artemis (lo que quede se procesa contra
# la base nueva) ni los pesajes de los nodos de balanzas, que viven en sus propias bases.
set -euo pipefail

PERFIL=demo
CONFIRMAR=si
while [ $# -gt 0 ]; do
  case "$1" in
    --si) CONFIRMAR=no; shift ;;
    --perfil) PERFIL=${2:-}; shift 2 ;;
    --) shift; break ;;
    *) echo "Opcion desconocida: $1 (ver el encabezado del script)"; exit 1 ;;
  esac
done
case "$PERFIL" in
  demo|base|performance) ;;
  *) echo "Perfil desconocido: $PERFIL (demo, base o performance)"; exit 1 ;;
esac
DATOS_SQL="$(cd "$(dirname "$0")/../.." && pwd)/performance/datos.sql"

PROYECTO=$(oc project -q)
BASE=${DB_NAME:-cargauy}
USUARIO_BASE=${DB_USER:-cargauy}
BASE_POSICIONES=${MONGO_DB:-cargauy}

if [ "$CONFIRMAR" = si ]; then
  echo "Se van a BORRAR todos los datos del central en el proyecto $PROYECTO y cargar el perfil $PERFIL:"
  echo "  - la base $BASE de PostgreSQL (empresas, usuarios y sus roles, viajes, casos, auditoria)"
  echo "  - la base $BASE_POSICIONES de MongoDB (posiciones de tracking)"
  echo "No se puede deshacer. Para seguir, escribir el nombre del proyecto:"
  read -r respuesta
  if [ "$respuesta" != "$PROYECTO" ]; then
    echo "No coincide: no se borro nada."
    exit 1
  fi
fi

echo "== Apagando el central"
# el autoescalado no sube un Deployment en 0 replicas, asi que no lo vuelve a encender en el medio
oc scale deployment/carga-uy --replicas=0
oc wait --for=delete pod -l app=carga-uy --timeout=180s 2>/dev/null || true

echo "== Borrando y creando la base $BASE"
POSTGRES=$(oc get pods -l deploymentconfig=postgres -o name | head -1)
# como superusuario por el socket local del contenedor; FORCE corta conexiones que hayan quedado
oc exec "$POSTGRES" -- psql -v ON_ERROR_STOP=1 -d postgres \
  -c "DROP DATABASE IF EXISTS $BASE WITH (FORCE)" \
  -c "CREATE DATABASE $BASE OWNER $USUARIO_BASE"

echo "== Borrando las posiciones de MongoDB"
# las credenciales de root ya estan en el entorno del contenedor de Mongo (las usa la imagen al iniciar)
oc exec deploy/mongo -- sh -c \
  'mongosh --quiet -u "$MONGO_INITDB_ROOT_USERNAME" -p "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin \
     --eval "db.getSiblingDB(\"'"$BASE_POSICIONES"'\").dropDatabase()"' > /dev/null

echo "== Perfil $PERFIL"
# con el Deployment en 0 replicas, cambiar el entorno no arranca ningun pod
if [ "$PERFIL" = demo ]; then
  oc set env deployment/carga-uy CARGAUY_DATOS=demo
else
  oc set env deployment/carga-uy CARGAUY_DATOS=base
fi

echo "== Encendiendo el central"
oc scale deployment/carga-uy --replicas=1
oc rollout status deployment/carga-uy --timeout=10m
oc wait --for=condition=Ready pod -l app=carga-uy --timeout=10m

if oc get secret carga-uy-demo > /dev/null 2>&1; then
  echo "== Elenco de la demo:"
  oc logs deployment/carga-uy | grep "demo elenco=" | tail -1 || echo "   (no aparece en el log: revisar el Secret carga-uy-demo)"
fi
if [ "$PERFIL" = performance ]; then
  echo "== Juego de datos de performance"
  # como superusuario, igual que el borrado; los parametros de despues de -- van a psql
  oc exec -i "$POSTGRES" -- psql -v ON_ERROR_STOP=1 -d "$BASE" "$@" < "$DATOS_SQL"
fi
echo "Listo: perfil $PERFIL cargado."
