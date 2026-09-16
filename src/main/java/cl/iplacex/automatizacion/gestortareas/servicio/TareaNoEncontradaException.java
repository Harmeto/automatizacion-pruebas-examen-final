package cl.iplacex.automatizacion.gestortareas.servicio;

/** Señala que se referenció una tarea que no existe. */
public class TareaNoEncontradaException extends RuntimeException {

    public TareaNoEncontradaException(Long id) {
        super("No existe la tarea con id " + id);
    }
}
