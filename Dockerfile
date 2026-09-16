# =============================================================================
# Imagen de la aplicación bajo prueba.
#
# Parte de un .jar ya construido en lugar de compilar dentro de la imagen: el
# pipeline empaqueta una sola vez y esa misma pieza es la que se prueba, se
# despliega y, si hace falta, se revierte. Compilar de nuevo aquí rompería esa
# garantía, porque el artefacto desplegado no sería exactamente el probado.
# =============================================================================

FROM eclipse-temurin:21-jre-alpine

# Ejecutar como usuario sin privilegios.
RUN addgroup -S gestor && adduser -S gestor -G gestor

WORKDIR /app

ARG JAR_FILE=target/gestor-tareas-1.0.0.jar
COPY ${JAR_FILE} aplicacion.jar

# Metadatos de auditoría: quedan grabados en la imagen y la aplicación los
# publica en /api/version, de modo que siempre se puede saber qué build
# concreto está atendiendo tráfico.
ARG APP_VERSION=desconocida
ARG BUILD_SHA=sin-commit
ARG BUILD_TIME=sin-fecha
ENV APP_VERSION=${APP_VERSION} \
    BUILD_SHA=${BUILD_SHA} \
    BUILD_TIME=${BUILD_TIME} \
    SERVER_PORT=8080

LABEL org.opencontainers.image.title="gestor-tareas" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.revision="${BUILD_SHA}"

USER gestor
EXPOSE 8080

HEALTHCHECK --interval=5s --timeout=3s --start-period=20s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "/app/aplicacion.jar"]
