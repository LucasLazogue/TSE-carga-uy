#!/bin/sh
# arma el truststore del nodo con el certificado que presenta el broker por la Route (passthrough + SNI).
# uso: NODO_TRUSTSTORE_CLAVE=... sh truststore.sh HOST_DEL_BROKER [confianza.p12]
# imprime la huella SHA-256: hay que compararla con la del keystore del broker antes de usar el archivo
set -eu

HOST=${1:?falta el host del broker, por ejemplo artemis-nodos-PROYECTO.apps.CLUSTER}
SALIDA=${2:-confianza.p12}
: "${NODO_TRUSTSTORE_CLAVE:?falta NODO_TRUSTSTORE_CLAVE, la clave con la que se protege el truststore}"

CERT=$(mktemp)
trap 'rm -f "$CERT"' EXIT

echo | openssl s_client -connect "$HOST:443" -servername "$HOST" 2>/dev/null | openssl x509 > "$CERT"
openssl x509 -in "$CERT" -noout -subject -enddate -fingerprint -sha256

rm -f "$SALIDA"
keytool -importcert -noprompt -alias broker -file "$CERT" \
    -keystore "$SALIDA" -storetype PKCS12 -storepass "$NODO_TRUSTSTORE_CLAVE"
echo "truststore en $SALIDA"
