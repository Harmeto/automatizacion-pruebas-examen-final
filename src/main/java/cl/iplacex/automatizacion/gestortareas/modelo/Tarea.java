package cl.iplacex.automatizacion.gestortareas.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad de dominio persistida en la base de datos.
 *
 * <p>Su ciclo de vida es el que verifican las pruebas de integración:
 * se crea vía API, se guarda en H2 y se recupera en la misma transacción.
 */
@Entity
public class Tarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;

    private String descripcion;

    @Enumerated(EnumType.STRING)
    private Prioridad prioridad;

    private boolean completada;

    private LocalDateTime fechaCreacion;

    /** Constructor sin argumentos requerido por JPA. */
    protected Tarea() {
    }

    public Tarea(String titulo, String descripcion, Prioridad prioridad) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.prioridad = prioridad;
        this.completada = false;
        this.fechaCreacion = LocalDateTime.now();
    }

    /** Marca la tarea como terminada. Operación idempotente. */
    public void completar() {
        this.completada = true;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public boolean isCompletada() {
        return completada;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setPrioridad(Prioridad prioridad) {
        this.prioridad = prioridad;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Tarea tarea)) {
            return false;
        }
        return id != null && Objects.equals(id, tarea.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
