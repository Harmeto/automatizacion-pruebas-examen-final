#!/usr/bin/env bash
#
# =============================================================================
# DEPLOYMENT PIPELINE — despliegue Blue-Green con rollback automático
# =============================================================================
#
# Etapas:
#   1. Empaquetar el artefacto y construir la imagen versionada
#   2. Desplegar en el slot INACTIVO (el que no recibe tráfico)
#   3. Esperar a que el slot nuevo esté sano
#   4. ACCEPTANCE TEST GATE: escenarios BDD contra el slot nuevo
#   5. Si el gate aprueba -> conmutar el tráfico
#      Si el gate rechaza -> ROLLBACK automático, el tráfico nunca se movió
#
# La decisión de promover no depende de que el despliegue "haya funcionado",
# sino de que la versión nueva cumpla los criterios de aceptación del negocio.
# Ese es el punto del gate.
#
# Uso: scripts/desplegar.sh <version>
#      scripts/desplegar.sh 1.1.0
# =============================================================================

set -euo pipefail

cd "$(dirname "$0")/.."

VERSION="${1:?Uso: $0 <version>}"
ARCHIVO_ENTORNO=".env"

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"

separador() { echo "================================================================"; }
etapa() { echo; separador; echo " ETAPA $1 — $2"; separador; }

SLOT_ACTIVO="$(grep -o 'Slot activo: .*' deploy/nginx/activo.conf | cut -d' ' -f3)"
if [[ "$SLOT_ACTIVO" == "blue" ]]; then
  SLOT_DESTINO="green"; PUERTO_DESTINO=8092
else
  SLOT_DESTINO="blue";  PUERTO_DESTINO=8091
fi

separador
echo " DESPLIEGUE BLUE-GREEN"
separador
echo "  versión a desplegar : ${VERSION}"
echo "  slot activo ahora   : ${SLOT_ACTIVO}  (sigue atendiendo usuarios)"
echo "  slot de destino     : ${SLOT_DESTINO}  (recibe la versión nueva)"

./scripts/registrar-auditoria.sh "DESPLIEGUE-INICIO" "$VERSION" "$SLOT_DESTINO" "iniciado"

# ---------------------------------------------------------------------------
etapa 1 "Empaquetado y construcción de la imagen"
# ---------------------------------------------------------------------------
COMMIT="$(git rev-parse --short HEAD 2>/dev/null || echo sin-commit)"
MOMENTO="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

./mvnw -B -q package -DskipTests
JAR="$(ls target/gestor-tareas-*.jar | head -1)"
echo "  artefacto: ${JAR}"

docker build -q \
  --build-arg "JAR_FILE=${JAR}" \
  --build-arg "APP_VERSION=${VERSION}" \
  --build-arg "BUILD_SHA=${COMMIT}" \
  --build-arg "BUILD_TIME=${MOMENTO}" \
  -t "gestor-tareas:${VERSION}" . > /dev/null
echo "  imagen construida: gestor-tareas:${VERSION}"

# ---------------------------------------------------------------------------
etapa 2 "Despliegue en el slot inactivo (${SLOT_DESTINO})"
# ---------------------------------------------------------------------------
# Se fija la versión del slot de destino conservando la del slot activo: la
# versión estable no se toca en ningún momento.
touch "$ARCHIVO_ENTORNO"
VERSION_BLUE_ACTUAL="$(grep -o '^VERSION_BLUE=.*'  "$ARCHIVO_ENTORNO" | cut -d= -f2 || true)"
VERSION_GREEN_ACTUAL="$(grep -o '^VERSION_GREEN=.*' "$ARCHIVO_ENTORNO" | cut -d= -f2 || true)"
VERSION_BLUE_ACTUAL="${VERSION_BLUE_ACTUAL:-1.0.0}"
VERSION_GREEN_ACTUAL="${VERSION_GREEN_ACTUAL:-1.0.0}"

if [[ "$SLOT_DESTINO" == "blue" ]]; then
  VERSION_BLUE_ACTUAL="$VERSION"
else
  VERSION_GREEN_ACTUAL="$VERSION"
fi

cat > "$ARCHIVO_ENTORNO" <<ENV
# Generado por scripts/desplegar.sh — versión desplegada en cada slot.
VERSION_BLUE=${VERSION_BLUE_ACTUAL}
VERSION_GREEN=${VERSION_GREEN_ACTUAL}
ENV

docker compose up -d --force-recreate "app-${SLOT_DESTINO}" > /dev/null 2>&1
echo "  contenedor app-${SLOT_DESTINO} recreado con la versión ${VERSION}"

# ---------------------------------------------------------------------------
etapa 3 "Verificación de salud del slot ${SLOT_DESTINO}"
# ---------------------------------------------------------------------------
SANO=false
for intento in $(seq 1 40); do
  if curl -sf --max-time 3 "http://localhost:${PUERTO_DESTINO}/actuator/health" 2>/dev/null \
      | grep -q '"status":"UP"'; then
    SANO=true
    echo "  slot ${SLOT_DESTINO} sano tras ${intento} intento(s)"
    break
  fi
  sleep 2
done

if [[ "$SANO" != true ]]; then
  echo "  el slot ${SLOT_DESTINO} nunca llegó a estar sano"
  echo
  echo "El tráfico sigue en ${SLOT_ACTIVO}: los usuarios no se vieron afectados."
  ./scripts/registrar-auditoria.sh "DESPLIEGUE-FALLIDO" "$VERSION" "$SLOT_DESTINO" "RECHAZADO: health check"
  exit 1
fi

echo "  versión que responde: $(curl -s "http://localhost:${PUERTO_DESTINO}/api/version")"

# ---------------------------------------------------------------------------
etapa 4 "ACCEPTANCE TEST GATE contra el slot ${SLOT_DESTINO}"
# ---------------------------------------------------------------------------
echo "  ejecutando escenarios BDD contra http://localhost:${PUERTO_DESTINO}"
echo

# Se borra el reporte anterior antes de correr. Si por cualquier motivo la
# suite no llegara a ejecutarse, el gate debe rechazar por falta de reporte y
# no aprobar leyendo el resultado de una ejecución pasada.
rm -rf target/cucumber-reports

GATE_APROBADO=true
./mvnw -B verify -Pacceptance \
  "-Dacceptance.url=http://localhost:${PUERTO_DESTINO}" \
  || GATE_APROBADO=false

# ---------------------------------------------------------------------------
etapa 5 "Decisión"
# ---------------------------------------------------------------------------
if [[ "$GATE_APROBADO" == true ]]; then
  echo "  el gate aprobó: se promueve la versión ${VERSION}"
  ./scripts/conmutar-trafico.sh "$SLOT_DESTINO"
  echo
  echo "DESPLIEGUE COMPLETADO. El tráfico ahora va al slot ${SLOT_DESTINO} (versión ${VERSION})."
  echo "El slot ${SLOT_ACTIVO} queda en stand-by para un rollback instantáneo."
  ./scripts/registrar-auditoria.sh "DESPLIEGUE-OK" "$VERSION" "$SLOT_DESTINO" "PROMOVIDO tras aprobar el gate"
  exit 0
fi

echo "  el gate RECHAZÓ la versión ${VERSION}"
echo "  el tráfico nunca se movió de ${SLOT_ACTIVO}, así que no hubo impacto en usuarios"
echo

# Aquí NO corresponde invocar rollback.sh. Ese script conmuta al OTRO slot,
# que en este punto es justamente el que acaba de ser rechazado. Como el gate
# se ejecuta ANTES de promover, la versión defectuosa nunca recibió tráfico:
# lo correcto es reafirmar el estado estable, no cambiarlo.
./scripts/conmutar-trafico.sh "$SLOT_ACTIVO" > /dev/null
echo "Estado confirmado: el tráfico sigue en ${SLOT_ACTIVO}."
echo "La versión ${VERSION} queda desplegada en ${SLOT_DESTINO} pero aislada, para diagnóstico."

./scripts/registrar-auditoria.sh "DESPLIEGUE-RECHAZADO" "$VERSION" "$SLOT_DESTINO" \
  "RECHAZADO por el Acceptance Gate; trafico intacto en ${SLOT_ACTIVO}"

exit 1
