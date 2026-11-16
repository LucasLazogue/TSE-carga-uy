#!/usr/bin/env bash
# Deja los datos del central de OpenShift como recien instalado: borra la base de PostgreSQL y las posiciones de
# MongoDB, y vuelve a encender el central, que crea el esquema y carga los datos de prueba. Si existe el Secret
# carga-uy-demo, carga ademas el elenco de la demo (ElencoDemo). Sirve para repetir la demo o una prueba desde el
# mismo punto. En Compose, lo mismo es: docker compose down -v && docker compose up -d
#
# Uso, con oc logueado en el proyecto del central:
#   ./reiniciar-datos.sh            pide confirmacion escribiendo el nombre del proyecto
#   ./reiniciar-datos.sh --si       sin preguntar (para scripts)
#
# No toca: los usuarios de LDAP (backoffice), las cuentas de gub.uy, la cola de Artemis (lo que quede se procesa contra
# la base nueva) ni los pesajes de los nodos de balanzas, que viven en sus propias bases.
set -euo pipefail

PROYECTO=$(oc project -q)
BASE=${DB_NAME:-cargauy}
USUARIO_BASE=${DB_USER:-cargauy}
BASE_POSICIONES=${MONGO_DB:-cargauy}

if [ "${1:-}" != "--si" ]; then
  echo "Se van a BORRAR todos los datos del central en el proyecto $PROYECTO:"
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

echo "== Encendiendo el central"
oc scale deployment/carga-uy --replicas=1
oc rollout status deployment/carga-uy --timeout=10m
oc wait --for=condition=Ready pod -l app=carga-uy --timeout=10m

if oc get secret carga-uy-demo > /dev/null 2>&1; then
  echo "== Elenco de la demo:"
  oc logs deployment/carga-uy | grep "demo elenco=" | tail -1 || echo "   (no aparece en el log: revisar el Secret carga-uy-demo)"
fi
echo "Listo: datos de prueba cargados."
