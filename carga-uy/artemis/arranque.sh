#!/bin/bash
# La imagen crea la instancia del broker solo la primera vez, y como la instancia vive en el volumen, la
# configuracion quedaria congelada en la de ese momento. En cada arranque se vuelve a copiar la del repositorio.
set -e
INSTANCIA=/var/lib/artemis-instance
mkdir -p "$INSTANCIA/etc-override"
cp /opt/carga-uy/etc/* "$INSTANCIA/etc-override/"
if [ -f "$INSTANCIA/etc/broker.xml" ]; then
    cp /opt/carga-uy/etc/* "$INSTANCIA/etc/"
fi
exec /docker-run.sh "$@"
