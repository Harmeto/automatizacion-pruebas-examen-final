# Guía de verificación

Cómo comprobar, punto por punto, que el proyecto cumple lo que pide el examen.

Cada sección indica **qué comando correr** y **qué deberías ver**. Todos los
comandos se ejecutan desde la raíz del proyecto:

```bash
cd /Users/harmeto/automatizacion
```

No hace falta configurar nada: el wrapper de Maven resuelve el entorno de Java por
su cuenta, y el ambiente de prueba ya está levantado.

> **El ambiente está corriendo ahora mismo.** Tres contenedores en los puertos
> 8090, 8091 y 8092, con la versión 1.0.0 activa en el slot azul. Al final de esta
> guía está cómo bajarlo.

---

## Antes de empezar: ver que hay algo vivo

```bash
./scripts/estado.sh
```

Deberías ver el slot azul recibiendo tráfico con la versión 1.0.0, y los dos slots
respondiendo. Abre también <http://localhost:8090> en el navegador: la cabecera
dice **«versión 1.0.0 · slot blue»**. Fíjate en ese dato, porque es el que va a
cambiar cuando despliegues.

---

## Actividad 1 — Repositorio y configuración Maven

### Se pedía: repositorio Git con flujo de ramas definido

```bash
git log --oneline --graph --all | head -40
```

**Qué deberías ver:** un historial con ramificaciones visibles, no una línea recta.
Cada `feature/*` y `fix/*` sale de `develop` y vuelve mediante un merge propio.

```bash
git branch -a
git tag -n1
```

**Qué deberías ver:** la estructura GitFlow (`main`, `develop`, varias `feature/*`,
`fix/*`, `release/*`) y dos versiones etiquetadas, `v1.0.0` y `v1.0.1`.

Hay una rama que **no** está integrada, `experimento/regresion-interfaz`. Es
deliberado y se explica en la Actividad 3.

### Se pedía: proyecto Maven con dependencias para pruebas

```bash
grep -A2 "artifactId>junit\|artifactId>selenium\|artifactId>cucumber\|artifactId>rest-assured" pom.xml | head -30
```

**Qué deberías ver:** JUnit, Selenium, Cucumber y RestAssured declarados, cada uno
con su versión explícita.

Para comprobar que el proyecto realmente construye:

```bash
./mvnw clean compile
```

**Qué deberías ver:** `BUILD SUCCESS`.

---

## Actividad 2 — Pipeline de integración continua

### Se pedía: stages de build y pruebas automatizadas

El pipeline está en `.github/workflows/ci.yml` y su equivalente en `Jenkinsfile`.
Puedes leer las etapas sin ejecutar nada:

```bash
grep -E "^  [a-z]+:|name:" .github/workflows/ci.yml | head -25
```

### Se pedía: al menos dos tipos de prueba (unitarias y de integración)

Esta es la comprobación importante, porque demuestra que los dos niveles están
**realmente separados** y no solo nombrados distinto.

**Solo las unitarias** (rápidas, sin base de datos):

```bash
./mvnw test
```

**Qué deberías ver:** `Tests run: 41, Failures: 0` y la línea
`All coverage checks have been met.` — esa línea es el gate de cobertura del 80 %
aprobando. Tarda unos segundos.

**Unitarias más integración** (levantan contexto y base de datos):

```bash
./mvnw verify
```

**Qué deberías ver:** primero las mismas 41 unitarias y después
`Tests run: 12, Failures: 0` correspondiente a las clases `*IT`. Tarda cerca de
medio minuto, y esa diferencia de tiempo es precisamente la razón por la que en el
pipeline van en etapas separadas.

**Análisis estático:**

```bash
./mvnw spotbugs:check
```

**Qué deberías ver:** `BugInstance size is 0` y `BUILD SUCCESS`.

### Se pedía: capturas de ejecución exitosa

El pipeline corrió de verdad en GitHub:

```bash
gh run list --limit 5
```

**Qué deberías ver:** varias corridas con estado `success`. También puedes abrirlas
en el navegador:

<https://github.com/Harmeto/automatizacion-pruebas-examen-final/actions>

La captura está en `docs/evidencias/07-ci-ejecucion-exitosa.png`.

---

## Actividad 3 — Tubería de despliegue

Aquí hay tres cosas distintas que comprobar. Conviene hacerlas en orden.

### 3.1 — Un despliegue que aprueba y promueve

```bash
./scripts/desplegar.sh 2.0.0
```

Tarda alrededor de un minuto. **Qué deberías ver, en este orden:**

1. Que el slot activo es `blue` y el de destino `green`
2. La construcción de la imagen
3. El slot verde pasando su verificación de salud
4. Un navegador ejecutando **9 escenarios** (verás los pasos en español)
5. `GATE APROBADO: los 9 escenarios cumplen los criterios de aceptación.`
6. `DESPLIEGUE COMPLETADO. El tráfico ahora va al slot green (versión 2.0.0).`

Ahora recarga <http://localhost:8090>. **La cabecera dice «versión 2.0.0 · slot
green»**. Es la misma dirección de antes: lo único que cambió es hacia dónde enruta
el balanceador.

```bash
./scripts/estado.sh
```

Fíjate en que el slot azul **sigue vivo** con la 1.0.0. No se apagó: quedó en
espera. Eso es lo que hace que el siguiente paso sea instantáneo.

### 3.2 — La reversión

```bash
./scripts/rollback.sh "estoy probando el rollback"
```

**Qué deberías ver:** que detecta el slot activo, comprueba que el destino esté
sano y conmuta. Termina con
`Rollback completado. El tráfico vuelve a la versión 1.0.0 (slot blue).`

Recarga <http://localhost:8090>: volvió a decir **«versión 1.0.0 · slot blue»**.
Tardó segundos porque la versión anterior nunca se detuvo.

### 3.3 — La barrera deteniendo una versión defectuosa

Esta es la comprobación que más vale la pena hacer, porque demuestra que el gate
sirve para algo.

```bash
git checkout experimento/regresion-interfaz
git merge develop
```

Esa rama tiene una regresión real: el identificador del mensaje de error del
formulario fue reemplazado por una clase de estilo. Primero comprueba que las
pruebas unitarias **no la detectan**:

```bash
./mvnw test
```

**Qué deberías ver:** `Tests run: 41, Failures: 0`. Todas pasan, con el defecto
presente. Ahora intenta desplegarla:

```bash
./scripts/desplegar.sh 2.1.0
```

**Qué deberías ver:**

1. Que se despliega en el slot inactivo y pasa la verificación de salud
2. `GATE RECHAZADO: hay escenarios que no cumplen los criterios de aceptación.`
   con los escenarios concretos que fallaron
3. `el tráfico nunca se movió de blue, así que no hubo impacto en usuarios`
4. El comando termina con código de salida 1

Recarga <http://localhost:8090>: **sigue en la versión 1.0.0**. La versión
defectuosa quedó desplegada en el otro slot, aislada, sin recibir tráfico.

Para volver al estado normal:

```bash
git checkout develop
```

### Registro de auditoría

Después de tus pruebas:

```bash
cat deploy/auditoria.log
```

**Qué deberías ver:** una línea por cada acción, con momento, versión, slot, commit,
rama y resultado. Ahí queda la traza de tus propios despliegues y reversiones.

---

## Documentación

Se pedía un README con descripción, estrategia de pruebas, instrucciones y capturas.
Está en la raíz: [`README.md`](../README.md).

Las evidencias de las ejecuciones documentadas están en `docs/evidencias/`:

```bash
ls docs/evidencias/
```

---

## Si algo se rompe

**El balanceador no responde:**

```bash
docker compose ps
./scripts/conmutar-trafico.sh blue
```

**Quieres volver al punto de partida:**

```bash
./scripts/levantar-ambiente.sh 1.0.0
```

Reconstruye ambos slots con la versión base y deja el tráfico en azul. Tarda un par
de minutos.

**Ver qué está pasando dentro de los contenedores:**

```bash
docker compose logs --tail=50
```

---

## Cuando termines

```bash
docker compose down -v
```

Detiene y elimina los tres contenedores y el volumen de configuración. Para volver a
levantar todo más adelante basta con `./scripts/levantar-ambiente.sh 1.0.0`.
