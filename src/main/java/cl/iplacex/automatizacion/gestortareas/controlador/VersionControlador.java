package cl.iplacex.automatizacion.gestortareas.controlador;

import cl.iplacex.automatizacion.gestortareas.config.InfoDespliegue;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone la identidad del despliegue que está atendiendo la petición.
 *
 * <p>Este endpoint es la pieza que hace demostrable el Blue-Green: antes del
 * switch un {@code curl} devuelve el slot antiguo, después del switch devuelve
 * el nuevo, y tras un rollback vuelve a devolver el antiguo. Sin él, el
 * despliegue funcionaría igual pero no habría evidencia que mostrar.
 */
@RestController
public class VersionControlador {

    private final InfoDespliegue infoDespliegue;

    public VersionControlador(InfoDespliegue infoDespliegue) {
        this.infoDespliegue = infoDespliegue;
    }

    @GetMapping("/api/version")
    public Map<String, String> version() {
        return Map.of(
                "version", infoDespliegue.getVersion(),
                "slot", infoDespliegue.getSlot(),
                "commitSha", infoDespliegue.getCommitSha(),
                "fechaBuild", infoDespliegue.getFechaBuild());
    }
}
