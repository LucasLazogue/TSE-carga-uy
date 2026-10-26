#!/bin/sh
set -eu

url=$1
claves=$2
pasadas=$3

# cada lectura trae la fecha fija ("fecha": "2026-10-15") o cuantos dias atras fue ("dias": -3),
# asi los pesajes caen cerca de hoy igual que los casos de los datos de prueba del central
lecturas() {
    echo "$1" | jq -c '[.lecturas[] | {
        idRegistro, matricula, pesoRegistrado,
        fechaHora: ((.fecha // (now + .dias * 86400 | localtime | strftime("%Y-%m-%d"))) + "T" + .hora + ":00")
    }]'
}

jq -c '.[]' "$pasadas" | while read -r pasada; do
    balanza=$(echo "$pasada" | jq -r '.balanza')
    clave=$(jq -r --arg b "$balanza" '.[$b] // empty' "$claves")
    if [ -z "$clave" ]; then
        echo "no hay clave para $balanza, falta darla de alta" >&2
        exit 1
    fi
    codigo=$(curl -s -o /dev/null -w '%{http_code}' -X POST -H "Authorization: Bearer $clave" \
        -H 'Content-Type: application/json' -d "$(lecturas "$pasada")" "$url/api/pesajes")
    if [ "$codigo" != 204 ]; then
        echo "$balanza no pudo enviar sus pesajes ($codigo)" >&2
        exit 1
    fi
    echo "$balanza envio $(echo "$pasada" | jq '.lecturas | length') lecturas"
done
