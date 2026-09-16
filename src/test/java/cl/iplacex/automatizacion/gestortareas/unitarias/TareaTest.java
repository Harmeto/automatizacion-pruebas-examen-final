package cl.iplacex.automatizacion.gestortareas.unitarias;

import static org.assertj.core.api.Assertions.assertThat;

import cl.iplacex.automatizacion.gestortareas.modelo.Prioridad;
import cl.iplacex.automatizacion.gestortareas.modelo.Tarea;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pruebas unitarias del comportamiento propio de la entidad. */
@DisplayName("Entidad Tarea")
class TareaTest {

    @Test
    @DisplayName("Una tarea nueva nace pendiente y con fecha de creación")
    void tareaNuevaNacePendiente() {
        Tarea tarea = new Tarea("Escribir pruebas", "unitarias", Prioridad.ALTA);

        assertThat(tarea.isCompletada()).isFalse();
        assertThat(tarea.getFechaCreacion()).isNotNull();
        assertThat(tarea.getTitulo()).isEqualTo("Escribir pruebas");
        assertThat(tarea.getDescripcion()).isEqualTo("unitarias");
        assertThat(tarea.getPrioridad()).isEqualTo(Prioridad.ALTA);
    }

    @Test
    @DisplayName("Completar dos veces deja el mismo resultado: la operación es idempotente")
    void completarEsIdempotente() {
        Tarea tarea = new Tarea("Desplegar", null, Prioridad.MEDIA);

        tarea.completar();
        tarea.completar();

        assertThat(tarea.isCompletada()).isTrue();
    }

    @Test
    @DisplayName("Dos tareas sin id persistido no se consideran iguales")
    void sinIdNoSonIguales() {
        Tarea una = new Tarea("A", null, Prioridad.BAJA);
        Tarea otra = new Tarea("A", null, Prioridad.BAJA);

        assertThat(una).isNotEqualTo(otra);
        assertThat(una).isEqualTo(una);
        assertThat(una).isNotEqualTo("no soy una tarea");
    }

    @Test
    @DisplayName("Los setters permiten editar los campos mutables")
    void permiteEditarCampos() {
        Tarea tarea = new Tarea("Original", "desc", Prioridad.BAJA);

        tarea.setTitulo("Editada");
        tarea.setDescripcion("nueva desc");
        tarea.setPrioridad(Prioridad.ALTA);

        assertThat(tarea.getTitulo()).isEqualTo("Editada");
        assertThat(tarea.getDescripcion()).isEqualTo("nueva desc");
        assertThat(tarea.getPrioridad()).isEqualTo(Prioridad.ALTA);
        assertThat(tarea.hashCode()).isEqualTo(new Tarea("x", null, Prioridad.BAJA).hashCode());
    }
}
