#!/bin/sh
set -eu

url=$1
clave_admin=$2
inventario=$3
claves=$4

[ -f "$claves" ] || echo '{}' > "$claves"

alta() {
    respuesta=$(curl -s -w '\n%{http_code}' -X POST -H "Authorization: Bearer $clave_admin" \
        -H 'Content-Type: application/json' -d "$2" "$url$1")
    codigo=$(echo "$respuesta" | tail -n 1)
    cuerpo=$(echo "$respuesta" | sed '$d')
    case $codigo in
        2*) echo "$cuerpo" ;;
        409) ;;
        *) echo "alta fallo en $1 ($codigo): $cuerpo" >&2; exit 1 ;;
    esac
}

jq -c '.[]' "$inventario" | while read -r peaje; do
    codigo_peaje=$(echo "$peaje" | jq -r '.codigo')
    alta /api/peajes "$(echo "$peaje" | jq -c 'del(.balanzas)')" > /dev/null
    echo "$peaje" | jq -c '.balanzas[]' | while read -r balanza; do
        respuesta=$(alta "/api/peajes/$codigo_peaje/balanzas" "$balanza")
        if [ -n "$respuesta" ]; then
            jq --argjson b "$respuesta" '. + {($b.codigo): $b.clave}' "$claves" > "$claves.tmp"
            mv "$claves.tmp" "$claves"
            echo "alta balanza $(echo "$respuesta" | jq -r '.codigo') en $codigo_peaje"
        fi
    done
done
