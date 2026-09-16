package cl.iplacex.automatizacion.gestortareas.controlador;

import cl.iplacex.automatizacion.gestortareas.config.InfoDespliegue;
import cl.iplacex.automatizacion.gestortareas.modelo.Prioridad;
import cl.iplacex.automatizacion.gestortareas.servicio.TareaInvalidaException;
import cl.iplacex.automatizacion.gestortareas.servicio.TareaServicio;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Interfaz web del gestor de tareas.
 *
 * <p>Es la superficie que automatizan los escenarios BDD con Selenium. Los
 * identificadores de los elementos HTML (ids estables, sin depender de
 * posición ni de texto) están pensados para que los Page Objects sean
 * robustos frente a cambios de maquetación (ME_4-3 §1.3).
 */
@Controller
public class TareaWebControlador {

    private final TareaServicio servicio;
    private final InfoDespliegue infoDespliegue;

    public TareaWebControlador(TareaServicio servicio, InfoDespliegue infoDespliegue) {
        this.servicio = servicio;
        this.infoDespliegue = infoDespliegue;
    }

    @GetMapping("/")
    public String ver(Model modelo) {
        return pintar(modelo, null);
    }

    @PostMapping("/tareas")
    public String crear(
            @RequestParam String titulo,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) Prioridad prioridad,
            Model modelo) {
        try {
            servicio.crear(titulo, descripcion, prioridad);
        } catch (TareaInvalidaException ex) {
            // El error se muestra en la misma página: es lo que verifica el
            // escenario BDD de validación.
            return pintar(modelo, ex.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/tareas/{id}/completar")
    public String completar(@PathVariable Long id) {
        servicio.completar(id);
        return "redirect:/";
    }

    @PostMapping("/tareas/{id}/eliminar")
    public String eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return "redirect:/";
    }

    private String pintar(Model modelo, String mensajeError) {
        modelo.addAttribute("tareas", servicio.listar());
        modelo.addAttribute("resumen", servicio.resumen());
        modelo.addAttribute("prioridades", Prioridad.values());
        modelo.addAttribute("info", infoDespliegue);
        modelo.addAttribute("mensajeError", mensajeError);
        return "tareas";
    }
}
