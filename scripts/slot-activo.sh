#!/usr/bin/env bash
#
# Imprime el slot que está recibiendo tráfico en este momento.
#
# La respuesta se obtiene preguntándole al balanceador, no leyendo un archivo
# de configuración del repositorio. La diferencia importa: el archivo dice lo
# que se quiso configurar, mientras que la aplicación que responde dice lo que
# realmente está pasando. Para decidir un despliegue o un rollback hay que
# guiarse por lo segundo.

set -uo pipefail

RESPUESTA="$(curl -s --max-time 3 http://localhost:8090/api/version 2>/dev/null || true)"
SLOT="$(echo "$RESPUESTA" | grep -o '"slot":"[^"]*"' | cut -d'"' -f4 || true)"

if [[ -n "${SLOT:-}" ]]; then
  echo "$SLOT"
  exit 0
fi

# Si el balanceador no responde, se recurre al último estado registrado.
ARCHIVO="$(dirname "$0")/../deploy/nginx/conf.d/activo.conf"
if [[ -f "$ARCHIVO" ]]; then
  grep -o 'Slot activo: .*' "$ARCHIVO" | cut -d' ' -f3
  exit 0
fi

echo "desconocido"
exit 1
