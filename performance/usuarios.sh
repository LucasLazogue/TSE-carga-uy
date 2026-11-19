#!/usr/bin/env bash
# Genera performance/usuarios.json para frontoffice.js: responsables vigentes con su empresa, funcionarios y numeros
# de empresa, leidos de la base del central en OpenShift. Usa la sesion de oc del proyecto del central.
# El archivo tiene cedulas: no se versiona (.gitignore).
#
#   KUBECONFIG=~/.kube/demo-central performance/usuarios.sh
set -euo pipefail
cd "$(dirname "$0")"

POD=$(oc get pods -l deploymentconfig=postgres -o name | head -1)
consulta() { oc exec "$POD" -- psql -U postgres -d cargauy -At -c "$1"; }

RESPONSABLES=$(consulta "select coalesce(json_agg(json_build_object('id', r.ciudadano_id, 'cedula', c.cedula, 'empresa', r.empresa_id)), '[]')
  from responsable r join ciudadano c on c.id = r.ciudadano_id
  where r.fechadesde <= current_date and (r.fechahasta is null or r.fechahasta >= current_date)")
FUNCIONARIOS=$(consulta "select coalesce(json_agg(json_build_object('id', id, 'cedula', cedula)), '[]') from funcionario")
EMPRESAS=$(consulta "select coalesce(json_agg(nroempresa), '[]') from empresa")

printf '{"responsables":%s,"funcionarios":%s,"nrosEmpresa":%s}\n' "$RESPONSABLES" "$FUNCIONARIOS" "$EMPRESAS" > usuarios.json
echo "usuarios.json: $(grep -o '"empresa"' usuarios.json | wc -l | tr -d ' ') responsables, $(echo "$FUNCIONARIOS" | grep -o '"id"' | wc -l | tr -d ' ') funcionarios"
