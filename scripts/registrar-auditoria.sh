#!/usr/bin/env bash
#
# Deja constancia de cada acción del pipeline de despliegue.
#
# La trazabilidad de un despliegue exige poder reconstruir después qué versión
# se desplegó, sobre qué commit, cuándo y con qué resultado. Sin este registro,
# un rollback exitoso no deja ninguna huella de que ocurrió.
#
# Uso: scripts/registrar-auditoria.sh <accion> <version> <slot> <resultado>

set -euo pipefail

cd "$(dirname "$0")/.."

ACCION="${1:?falta la acción}"
VERSION="${2:?falta la versión}"
SLOT="${3:?falta el slot}"
RESULTADO="${4:?falta el resultado}"

# Se escribe en un archivo de estado en tiempo de ejecución, no en la evidencia
# versionada. Si el script escribiera sobre el archivo del repositorio, cada
# despliegue dejaría el árbol de trabajo sucio y bloquearía el cambio de rama,
# que es justamente lo que hay que hacer para probar el rechazo del gate.
REGISTRO="deploy/auditoria.log"
mkdir -p "$(dirname "$REGISTRO")"

COMMIT="$(git rev-parse --short HEAD 2>/dev/null || echo sin-commit)"
RAMA="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo sin-rama)"
MOMENTO="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

printf '%s | %-18s | version=%-18s | slot=%-5s | commit=%-8s | rama=%-28s | %s\n' \
  "$MOMENTO" "$ACCION" "$VERSION" "$SLOT" "$COMMIT" "$RAMA" "$RESULTADO" >> "$REGISTRO"
