package cl.iplacex.automatizacion.gestortareas.unitarias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.iplacex.automatizacion.gestortareas.modelo.Prioridad;
import cl.iplacex.automatizacion.gestortareas.modelo.ResumenTareas;
import cl.iplacex.automatizacion.gestortareas.modelo.Tarea;
import cl.iplacex.automatizacion.gestortareas.repositorio.TareaRepositorio;
import cl.iplacex.automatizacion.gestortareas.servicio.TareaInvalidaException;
import cl.iplacex.automatizacion.gestortareas.servicio.TareaNoEncontradaException;
import cl.iplacex.automatizacion.gestortareas.servicio.TareaServicio;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas unitarias de las reglas de negocio.
 *
 * <p>El repositorio se sustituye por un mock de Mockito: estas pruebas no
 * tocan la base de datos ni levantan el contexto de Spring. Esa es
 * precisamente la diferencia con las pruebas de integración, y lo que les
 * permite correr en la etapa de commit del pipeline.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Reglas de negocio del servicio de tareas")
class TareaServicioTest {

    @Mock
    private TareaRepositorio repositorio;

    @InjectMocks
    private TareaServicio servicio;

    @Nested
    @DisplayName("Al crear una tarea")
    class AlCrear {

        @Test
        @DisplayName("Guarda la tarea cuando los datos son válidos")
        void guardaTareaValida() {
            when(repositorio.findByTituloIgnoreCase(anyString())).thenReturn(Optional.empty());
            when(repositorio.save(any(Tarea.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

            Tarea creada = servicio.crear("Revisar pipeline", "stage de build", Prioridad.ALTA);

            assertThat(creada.getTitulo()).isEqualTo("Revisar pipeline");
            assertThat(creada.isCompletada()).isFalse();
            verify(repositorio).save(any(Tarea.class));
        }

        @Test
        @DisplayName("Recorta los espacios del título antes de guardarlo")
        void recortaEspacios() {
            when(repositorio.findByTituloIgnoreCase(anyString())).thenReturn(Optional.empty());
            when(repositorio.save(any(Tarea.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

            servicio.crear("   Con espacios   ", null, Prioridad.BAJA);

            ArgumentCaptor<Tarea> capturada = ArgumentCaptor.forClass(Tarea.class);
            verify(repositorio).save(capturada.capture());
            assertThat(capturada.getValue().getTitulo()).isEqualTo("Con espacios");
        }

        @Test
        @DisplayName("Asigna prioridad MEDIA cuando no se indica ninguna")
        void asignaPrioridadPorDefecto() {
            when(repositorio.findByTituloIgnoreCase(anyString())).thenReturn(Optional.empty());
            when(repositorio.save(any(Tarea.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

            Tarea creada = servicio.crear("Sin prioridad", null, null);

            assertThat(creada.getPrioridad()).isEqualTo(Prioridad.MEDIA);
        }

        @Test
        @DisplayName("Rechaza un título nulo, vacío o compuesto solo por espacios")
        void rechazaTituloVacio() {
            assertThatThrownBy(() -> servicio.crear(null, null, Prioridad.ALTA))
                    .isInstanceOf(TareaInvalidaException.class)
                    .hasMessageContaining("obligatorio");

            assertThatThrownBy(() -> servicio.crear("   ", null, Prioridad.ALTA))
                    .isInstanceOf(TareaInvalidaException.class)
                    .hasMessageContaining("obligatorio");

            // La regla debe cortar ANTES de tocar el repositorio.
            verify(repositorio, never()).save(any(Tarea.class));
        }

        @Test
        @DisplayName("Rechaza un título que excede los 100 caracteres")
        void rechazaTituloMuyLargo() {
            String tituloLargo = "x".repeat(101);

            assertThatThrownBy(() -> servicio.crear(tituloLargo, null, Prioridad.ALTA))
                    .isInstanceOf(TareaInvalidaException.class)
                    .hasMessageContaining("100");
        }

        @Test
        @DisplayName("Acepta exactamente 100 caracteres: el límite es inclusivo")
        void aceptaTituloEnElLimite() {
            when(repositorio.findByTituloIgnoreCase(anyString())).thenReturn(Optional.empty());
            when(repositorio.save(any(Tarea.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

            Tarea creada = servicio.crear("x".repeat(100), null, Prioridad.BAJA);

            assertThat(creada.getTitulo()).hasSize(100);
        }

        @Test
        @DisplayName("Rechaza un título duplicado sin importar mayúsculas")
        void rechazaDuplicado() {
            Tarea existente = new Tarea("Desplegar", null, Prioridad.ALTA);
            when(repositorio.findByTituloIgnoreCase("DESPLEGAR")).thenReturn(Optional.of(existente));

            assertThatThrownBy(() -> servicio.crear("DESPLEGAR", null, Prioridad.BAJA))
                    .isInstanceOf(TareaInvalidaException.class)
                    .hasMessageContaining("Ya existe");

            verify(repositorio, never()).save(any(Tarea.class));
        }
    }

    @Nested
    @DisplayName("Al consultar y modificar")
    class AlConsultar {

        @Test
        @DisplayName("Lista todas las tareas del repositorio")
        void listaTareas() {
            when(repositorio.findAll()).thenReturn(List.of(
                    new Tarea("A", null, Prioridad.ALTA),
                    new Tarea("B", null, Prioridad.BAJA)));

            assertThat(servicio.listar()).hasSize(2);
        }

        @Test
        @DisplayName("Buscar una tarea inexistente lanza la excepción de dominio")
        void buscarInexistenteFalla() {
            when(repositorio.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.buscar(99L))
                    .isInstanceOf(TareaNoEncontradaException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("Completar marca la tarea y la persiste")
        void completaTarea() {
            Tarea tarea = new Tarea("Pendiente", null, Prioridad.MEDIA);
            when(repositorio.findById(1L)).thenReturn(Optional.of(tarea));
            when(repositorio.save(tarea)).thenReturn(tarea);

            Tarea resultado = servicio.completar(1L);

            assertThat(resultado.isCompletada()).isTrue();
            verify(repositorio).save(tarea);
        }

        @Test
        @DisplayName("Eliminar una tarea existente delega el borrado al repositorio")
        void eliminaTarea() {
            when(repositorio.existsById(1L)).thenReturn(true);

            servicio.eliminar(1L);

            verify(repositorio).deleteById(1L);
        }

        @Test
        @DisplayName("Eliminar una tarea inexistente falla sin tocar el repositorio")
        void eliminarInexistenteFalla() {
            when(repositorio.existsById(42L)).thenReturn(false);

            assertThatThrownBy(() -> servicio.eliminar(42L))
                    .isInstanceOf(TareaNoEncontradaException.class);

            verify(repositorio, never()).deleteById(42L);
        }

        @Test
        @DisplayName("El resumen combina los conteos del repositorio")
        void calculaResumen() {
            when(repositorio.count()).thenReturn(5L);
            when(repositorio.countByCompletadaTrue()).thenReturn(2L);

            ResumenTareas resumen = servicio.resumen();

            assertThat(resumen.total()).isEqualTo(5);
            assertThat(resumen.completadas()).isEqualTo(2);
            assertThat(resumen.pendientes()).isEqualTo(3);
            assertThat(resumen.porcentajeAvance()).isEqualTo(40);
        }
    }
}
