package cl.iplacex.automatizacion.gestortareas.modelo;

/**
 * Vista agregada del estado de las tareas.
 *
 * <p>El porcentaje de avance es lógica de negocio pura, sin dependencias:
 * el caso ideal para pruebas unitarias con aserciones exactas.
 *
 * @param total        cantidad total de tareas registradas
 * @param completadas  cantidad de tareas ya terminadas
 * @param pendientes   cantidad de tareas aún abiertas
 * @param porcentajeAvance porcentaje completado, redondeado a un entero 0..100
 */
public record ResumenTareas(long total, long completadas, long pendientes, int porcentajeAvance) {

    /** Construye el resumen calculando el avance. Un tablero vacío avanza 0 %. */
    public static ResumenTareas calcular(long total, long completadas) {
        if (total < 0 || completadas < 0) {
            throw new IllegalArgumentException("Los conteos no pueden ser negativos");
        }
        if (completadas > total) {
            throw new IllegalArgumentException("Las completadas no pueden superar el total");
        }
        int porcentaje = (total == 0) ? 0 : (int) Math.round((completadas * 100.0) / total);
        return new ResumenTareas(total, completadas, total - completadas, porcentaje);
    }
}
