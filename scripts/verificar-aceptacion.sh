#!/usr/bin/env bash
#
# Verifica el resultado del Acceptance Test Gate.
#
# Existe por una razón concreta: el motor de suite de JUnit Platform no
# propaga los resultados de los escenarios de Cucumber al reporte agregado
# de Failsafe, que los informa como "Tests run: 0" y deja pasar el build
# aunque haya escenarios fallidos. Un gate que no puede detener el pipeline
# no es un gate, así que la verificación se hace sobre el reporte que
# Cucumber sí publica correctamente.
#
# Uso:  scripts/verificar-aceptacion.sh [ruta-al-reporte]
# Sale con 0 si todos los escenarios pasaron, con 1 en cualquier otro caso.

set -euo pipefail

REPORTE="${1:-target/cucumber-reports/acceptance.xml}"

if [[ ! -f "$REPORTE" ]]; then
  echo "GATE RECHAZADO: no se generó el reporte de aceptación en $REPORTE"
  echo "                (la suite no llegó a ejecutarse)"
  exit 1
fi

cabecera="$(grep -o '<testsuite[^>]*>' "$REPORTE" | head -1)"

leer_atributo() {
  echo "$cabecera" | grep -o "$1=\"[0-9]*\"" | grep -o '[0-9]*' || echo 0
}

total="$(leer_atributo tests)"
fallidos="$(leer_atributo failures)"
errores="$(leer_atributo errors)"
omitidos="$(leer_atributo skipped)"

echo "----------------------------------------------------------------"
echo " Acceptance Test Gate"
echo "----------------------------------------------------------------"
echo "  escenarios ejecutados : $total"
echo "  fallidos              : $fallidos"
echo "  con error             : $errores"
echo "  omitidos              : $omitidos"
echo "----------------------------------------------------------------"

if [[ "$total" -eq 0 ]]; then
  echo "GATE RECHAZADO: no se ejecutó ningún escenario."
  exit 1
fi

if [[ "$fallidos" -ne 0 || "$errores" -ne 0 ]]; then
  echo "GATE RECHAZADO: hay escenarios que no cumplen los criterios de aceptación."
  echo
  echo "Escenarios con problemas:"
  # El espacio previo evita capturar también el atributo classname.
  grep -B1 '<failure\|<error' "$REPORTE" \
    | grep -o ' name="[^"]*"' \
    | sed 's/ name="/  - /; s/"$//' || true
  exit 1
fi

echo "GATE APROBADO: los $total escenarios cumplen los criterios de aceptación."
exit 0
