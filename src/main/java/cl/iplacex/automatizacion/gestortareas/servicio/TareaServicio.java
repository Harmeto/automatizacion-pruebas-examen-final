package cl.iplacex.automatizacion.gestortareas.servicio;

import cl.iplacex.automatizacion.gestortareas.modelo.Prioridad;
import cl.iplacex.automatizacion.gestortareas.modelo.ResumenTareas;
import cl.iplacex.automatizacion.gestortareas.modelo.Tarea;
import cl.iplacex.automatizacion.gestortareas.repositorio.TareaRepositorio;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio del gestor de tareas.
 *
 * <p>Concentra deliberadamente toda la lógica verificable (validaciones,
 * unicidad, cálculo de avance) para que las pruebas unitarias tengan un
 * objetivo claro y las de integración un comportamiento observable.
 */
@Service
public class TareaServicio {

    /** Límite de longitud del título, alineado con la validación del formulario web. */
    static final int LARGO_MAXIMO_TITULO = 100;

    private final TareaRepositorio repositorio;

    public TareaServicio(TareaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Registra una tarea nueva.
     *
     * @throws TareaInvalidaException si el título es vacío, excede el largo máximo
     *                               o ya existe otra tarea con el mismo título
     */
    @Transactional
    public Tarea crear(String titulo, String descripcion, Prioridad prioridad) {
        String tituloNormalizado = (titulo == null) ? "" : titulo.trim();

        if (tituloNormalizado.isEmpty()) {
            throw new TareaInvalidaException("El título de la tarea es obligatorio");
        }
        if (tituloNormalizado.length() > LARGO_MAXIMO_TITULO) {
            throw new TareaInvalidaException(
                    "El título no puede superar los " + LARGO_MAXIMO_TITULO + " caracteres");
        }
        if (repositorio.findByTituloIgnoreCase(tituloNormalizado).isPresent()) {
            throw new TareaInvalidaException("Ya existe una tarea con el título: " + tituloNormalizado);
        }

        Prioridad prioridadEfectiva = (prioridad == null) ? Prioridad.MEDIA : prioridad;
        return repositorio.save(new Tarea(tituloNormalizado, descripcion, prioridadEfectiva));
    }

    @Transactional(readOnly = true)
    public List<Tarea> listar() {
        return repositorio.findAll();
    }

    @Transactional(readOnly = true)
    public Tarea buscar(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new TareaNoEncontradaException(id));
    }

    /** Marca una tarea como completada. Repetir la operación no altera el resultado. */
    @Transactional
    public Tarea completar(Long id) {
        Tarea tarea = buscar(id);
        tarea.completar();
        return repositorio.save(tarea);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repositorio.existsById(id)) {
            throw new TareaNoEncontradaException(id);
        }
        repositorio.deleteById(id);
    }

    /** Devuelve el estado agregado del tablero. */
    @Transactional(readOnly = true)
    public ResumenTareas resumen() {
        return ResumenTareas.calcular(repositorio.count(), repositorio.countByCompletadaTrue());
    }
}
