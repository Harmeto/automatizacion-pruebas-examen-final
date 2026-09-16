package cl.iplacex.automatizacion.gestortareas.controlador;

import cl.iplacex.automatizacion.gestortareas.modelo.Prioridad;
import cl.iplacex.automatizacion.gestortareas.modelo.ResumenTareas;
import cl.iplacex.automatizacion.gestortareas.modelo.Tarea;
import cl.iplacex.automatizacion.gestortareas.servicio.TareaInvalidaException;
import cl.iplacex.automatizacion.gestortareas.servicio.TareaNoEncontradaException;
import cl.iplacex.automatizacion.gestortareas.servicio.TareaServicio;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST del gestor de tareas.
 *
 * <p>Es la superficie que verifican las pruebas de integración con RestAssured
 * y MockMvc: contrato HTTP, códigos de estado y serialización.
 */
@RestController
@RequestMapping("/api/tareas")
public class TareaApiControlador {

    private final TareaServicio servicio;

    public TareaApiControlador(TareaServicio servicio) {
        this.servicio = servicio;
    }

    /** Petición de creación de una tarea. */
    public record CrearTareaRequest(String titulo, String descripcion, Prioridad prioridad) {
    }

    @GetMapping
    public List<Tarea> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public Tarea buscar(@PathVariable Long id) {
        return servicio.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Tarea> crear(@RequestBody CrearTareaRequest peticion) {
        Tarea creada = servicio.crear(peticion.titulo(), peticion.descripcion(), peticion.prioridad());
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PostMapping("/{id}/completar")
    public Tarea completar(@PathVariable Long id) {
        return servicio.completar(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/resumen")
    public ResumenTareas resumen() {
        return servicio.resumen();
    }

    @ExceptionHandler(TareaInvalidaException.class)
    public ResponseEntity<Map<String, String>> manejarInvalida(TareaInvalidaException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(TareaNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> manejarNoEncontrada(TareaNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }
}
