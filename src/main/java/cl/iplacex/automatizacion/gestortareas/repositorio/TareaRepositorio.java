package cl.iplacex.automatizacion.gestortareas.repositorio;

import cl.iplacex.automatizacion.gestortareas.modelo.Tarea;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Acceso a datos de las tareas.
 *
 * <p>Al ser una interfaz, el servicio puede probarse unitariamente sustituyéndola
 * por un mock de Mockito, sin levantar base de datos alguna (ME_3-3 §3.3).
 */
@Repository
public interface TareaRepositorio extends JpaRepository<Tarea, Long> {

    /** Búsqueda insensible a mayúsculas, usada para impedir títulos duplicados. */
    Optional<Tarea> findByTituloIgnoreCase(String titulo);

    long countByCompletadaTrue();
}
