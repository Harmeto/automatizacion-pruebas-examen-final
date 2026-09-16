#!/usr/bin/env bash
#
# Conmuta el tráfico del balanceador hacia el slot indicado.
#
# Reescribe la configuración del proxy y la recarga en caliente. Recargar no
# corta las conexiones en curso, de modo que el cambio de versión es
# transparente para quien esté usando la aplicación.
#
# Uso: scripts/conmutar-trafico.sh <blue|green>

set -euo pipefail

cd "$(dirname "$0")/.."

DESTINO="${1:-}"
ARCHIVO_ACTIVO="deploy/nginx/conf.d/activo.conf"

if [[ "$DESTINO" != "blue" && "$DESTINO" != "green" ]]; then
  echo "Uso: $0 <blue|green>"
  exit 1
fi

mkdir -p "$(dirname "$ARCHIVO_ACTIVO")"

cat > "$ARCHIVO_ACTIVO" <<CONF
# Generado por scripts/conmutar-trafico.sh — no editar a mano.
# Slot activo: ${DESTINO}
server {
    listen 80;

    # Tiempos cortos: si el slot activo deja de responder se nota de
    # inmediato, en vez de dejar la petición colgada.
    proxy_connect_timeout 3s;
    proxy_send_timeout   10s;
    proxy_read_timeout   10s;

    location / {
        proxy_pass http://app-${DESTINO}:8080;
        proxy_set_header Host              \$host;
        proxy_set_header X-Real-IP         \$remote_addr;
        proxy_set_header X-Forwarded-For   \$proxy_add_x_forwarded_for;
        proxy_set_header X-Slot-Destino    ${DESTINO};
    }
}
CONF

# La configuración se instala dentro del contenedor, sobre el volumen de
# Docker. La copia del repositorio queda como registro versionado de cuál fue
# el último estado aplicado, pero no es la que lee nginx.
docker compose exec -T balanceador sh -c 'cat > /etc/nginx/conf.d/activo.conf' < "$ARCHIVO_ACTIVO"

# La imagen trae un default.conf que también escucha en el puerto 80 y se
# quedaría con las peticiones al ser el primer server block. Se retira.
docker compose exec -T balanceador rm -f /etc/nginx/conf.d/default.conf

# Recarga en caliente: nginx relee la configuración sin reiniciar el proceso.
docker compose exec -T balanceador nginx -s reload 2>/dev/null

# La recarga es asíncrona: nginx levanta workers nuevos y retira los viejos a
# medida que terminan sus peticiones en curso. Devolver el control antes de
# que el cambio sea efectivo haría que una comprobación inmediata leyera aún
# el slot anterior, así que se confirma preguntándole al propio balanceador.
for intento in $(seq 1 20); do
  SLOT_QUE_RESPONDE="$(curl -s --max-time 2 http://localhost:8090/api/version 2>/dev/null \
    | grep -o '"slot":"[^"]*"' | cut -d'"' -f4 || true)"

  if [[ "$SLOT_QUE_RESPONDE" == "$DESTINO" ]]; then
    echo "Tráfico conmutado al slot: ${DESTINO} (confirmado al intento ${intento})"
    exit 0
  fi
  sleep 0.5
done

echo "ADVERTENCIA: la configuración apunta a ${DESTINO} pero el balanceador"
echo "             aún responde desde '${SLOT_QUE_RESPONDE:-sin respuesta}'."
exit 1
