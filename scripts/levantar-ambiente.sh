#!/usr/bin/env bash
#
# Levanta el ambiente de prueba desde cero con la versión base en ambos slots
# y el tráfico apuntando a blue.
#
# Uso: scripts/levantar-ambiente.sh [version-base]

set -euo pipefail

cd "$(dirname "$0")/.."

VERSION="${1:-1.0.0}"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"

echo "Construyendo la versión base ${VERSION}..."
./mvnw -B -q package -DskipTests -Djacoco.skip=true
JAR="$(ls target/gestor-tareas-*.jar | head -1)"

docker build -q \
  --build-arg "JAR_FILE=${JAR}" \
  --build-arg "APP_VERSION=${VERSION}" \
  --build-arg "BUILD_SHA=$(git rev-parse --short HEAD 2>/dev/null || echo sin-commit)" \
  --build-arg "BUILD_TIME=$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  -t "gestor-tareas:${VERSION}" . > /dev/null

cat > .env <<ENV
# Generado por scripts/levantar-ambiente.sh
VERSION_BLUE=${VERSION}
VERSION_GREEN=${VERSION}
ENV

./scripts/conmutar-trafico.sh blue 2>/dev/null || {
  # En el primer arranque nginx aún no existe, así que se escribe la
  # configuración directamente y se levanta todo después.
  sed -i.bak 's/app-green/app-blue/; s/Slot activo: green/Slot activo: blue/' deploy/nginx/conf.d/activo.conf
  rm -f deploy/nginx/conf.d/activo.conf.bak
}

docker compose up -d
echo "Esperando a que el ambiente responda..."
for i in $(seq 1 40); do
  sleep 2
  if curl -sf --max-time 3 http://localhost:8090/api/version >/dev/null 2>&1; then
    echo "Ambiente disponible en http://localhost:8090"
    ./scripts/estado.sh
    exit 0
  fi
done

echo "El ambiente no respondió a tiempo."
docker compose ps
exit 1
