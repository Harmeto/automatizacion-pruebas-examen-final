package cl.iplacex.automatizacion.gestortareas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Metadatos de auditoría del despliegue.
 *
 * <p>La auditoría de despliegues exige registrar el identificador de build,
 * el commit, la versión y la rama de origen como evidencia verificable. Exponerlos
 * por HTTP cumple además un segundo propósito práctico: es lo que permite
 * demostrar con un simple {@code curl} qué slot (blue o green) está sirviendo
 * tráfico en cada momento, y por lo tanto evidenciar el switch y el rollback.
 */
@Component
public class InfoDespliegue {

    private final String version;
    private final String slot;
    private final String commitSha;
    private final String fechaBuild;

    public InfoDespliegue(
            @Value("${APP_VERSION:${info.app.version:desconocida}}") String version,
            @Value("${APP_SLOT:local}") String slot,
            @Value("${BUILD_SHA:sin-commit}") String commitSha,
            @Value("${BUILD_TIME:sin-fecha}") String fechaBuild) {
        this.version = version;
        this.slot = slot;
        this.commitSha = commitSha;
        this.fechaBuild = fechaBuild;
    }

    public String getVersion() {
        return version;
    }

    /** Identifica el entorno Blue-Green que atiende la petición. */
    public String getSlot() {
        return slot;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public String getFechaBuild() {
        return fechaBuild;
    }
}
