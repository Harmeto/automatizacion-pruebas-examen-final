#!/usr/bin/env bash
#
# Revierte el ambiente a la versión anterior.
#
# El rollback es instantáneo porque el slot anterior nunca se apagó: quedó en
# stand-by sirviendo la versión estable. Revertir es, literalmente, devolver
# el tráfico a donde estaba.
#
# Puede invocarse de dos formas:
#   · automáticamente, desde desplegar.sh, cuando el Acceptance Gate rechaza
#   · a mano, por un operador, si se detecta un problema después del switch
#
# Uso: scripts/rollback.sh [motivo]

set -euo pipefail

cd "$(dirname "$0")/.."

MOTIVO="${1:-rollback solicitado manualmente}"

SLOT_ACTUAL="$(grep -o 'Slot activo: .*' deploy/nginx/conf.d/activo.conf | cut -d' ' -f3)"

if [[ "$SLOT_ACTUAL" == "blue" ]]; then
  SLOT_ANTERIOR="green"
else
  SLOT_ANTERIOR="blue"
fi

echo "================================================================"
echo " ROLLBACK"
echo "================================================================"
echo "  motivo        : ${MOTIVO}"
echo "  slot actual   : ${SLOT_ACTUAL}"
echo "  se revierte a : ${SLOT_ANTERIOR}"
echo

# Antes de devolver el tráfico hay que confirmar que el slot de destino está
# realmente sano. Revertir hacia algo caído sería cambiar un problema por otro.
PUERTO_ANTERIOR=$([[ "$SLOT_ANTERIOR" == "blue" ]] && echo 8091 || echo 8092)

if ! curl -sf --max-time 5 "http://localhost:${PUERTO_ANTERIOR}/actuator/health" | grep -q '"status":"UP"'; then
  echo "ROLLBACK ABORTADO: el slot ${SLOT_ANTERIOR} no está sano."
  echo "                   Se mantiene el tráfico en ${SLOT_ACTUAL}."
  ./scripts/registrar-auditoria.sh "ROLLBACK" "-" "$SLOT_ANTERIOR" "ABORTADO: destino no sano"
  exit 1
fi

VERSION_ANTERIOR="$(curl -s --max-time 5 "http://localhost:${PUERTO_ANTERIOR}/api/version" \
  | grep -o '"version":"[^"]*"' | cut -d'"' -f4 || echo desconocida)"

./scripts/conmutar-trafico.sh "$SLOT_ANTERIOR"

echo
echo "Rollback completado. El tráfico vuelve a la versión ${VERSION_ANTERIOR} (slot ${SLOT_ANTERIOR})."

./scripts/registrar-auditoria.sh "ROLLBACK" "$VERSION_ANTERIOR" "$SLOT_ANTERIOR" "COMPLETADO: ${MOTIVO}"
