#!/usr/bin/env bash
#
# Muestra el estado del ambiente: qué slot recibe tráfico y qué versión
# sirve cada uno. Es el comando que se usa para capturar evidencia antes y
# después de un despliegue o un rollback.

set -uo pipefail

cd "$(dirname "$0")/.."

slot_activo() {
  grep -o 'Slot activo: .*' deploy/nginx/activo.conf | cut -d' ' -f3
}

consultar() {
  curl -s --max-time 3 "$1/api/version" 2>/dev/null || echo '{"error":"sin respuesta"}'
}

echo "================================================================"
echo " Ambiente de prueba — estado actual"
echo "================================================================"
echo "  slot activo (recibe tráfico) : $(slot_activo)"
echo
echo "  a través del balanceador :8090"
echo "    $(consultar http://localhost:8090)"
echo
echo "  slot blue directo        :8091"
echo "    $(consultar http://localhost:8091)"
echo
echo "  slot green directo       :8092"
echo "    $(consultar http://localhost:8092)"
echo "================================================================"
