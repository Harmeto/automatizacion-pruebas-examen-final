package cl.iplacex.automatizacion.gestortareas.servicio;

/** Señala una violación de las reglas de negocio al crear o modificar una tarea. */
public class TareaInvalidaException extends RuntimeException {

    public TareaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
