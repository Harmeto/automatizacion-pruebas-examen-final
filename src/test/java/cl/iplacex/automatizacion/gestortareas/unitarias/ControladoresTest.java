package cl.iplacex.automatizacion.gestortareas.unitarias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import cl.iplacex.automatizacion.gestortareas.config.InfoDespliegue;
import cl.iplacex.automatizacion.gestortareas.controlador.TareaApiControlador;
import cl.iplacex.automatizacion.gestortareas.controlador.TareaWebControlador;
import cl.iplacex.automatizacion.gestortareas.controlador.VersionControlador;
import cl.iplacex.automatizacion.gestortareas.modelo.Prioridad;
import cl.iplacex.automatizacion.gestortareas.modelo.ResumenTareas;
import cl.iplacex.automatizacion.gestortareas.modelo.Tarea;
import cl.iplacex.automatizacion.gestortareas.servicio.TareaInvalidaException;
import cl.iplacex.automatizacion.gestortareas.servicio.TareaNoEncontradaException;
import cl.iplacex.automatizacion.gestortareas.servicio.TareaServicio;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 * Pruebas unitarias de la capa de presentación.
 *
 * <p>Usan {@code standaloneSetup}: instancian el controlador con un servicio
 * simulado, sin levantar el contexto de Spring ni la base de datos. Verifican
 * el contrato HTTP (rutas, códigos de estado, modelo de la vista) de forma
 * aislada y rápida.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Capa de controladores")
class ControladoresTest {

    @Mock
    private TareaServicio servicio;

    private Tarea tareaEjemplo;

    @BeforeEach
    void prepararDatos() {
        tareaEjemplo = new Tarea("Ejemplo", "descripción", Prioridad.ALTA);
    }

    @Nested
    @DisplayName("API REST")
    class Api {

        private MockMvc mockMvc;

        @BeforeEach
        void montar() {
            mockMvc = MockMvcBuilders.standaloneSetup(new TareaApiControlador(servicio)).build();
        }

        @Test
        @DisplayName("GET /api/tareas devuelve la colección")
        void listar() throws Exception {
            when(servicio.listar()).thenReturn(List.of(tareaEjemplo));

            mockMvc.perform(get("/api/tareas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].titulo").value("Ejemplo"));
        }

        @Test
        @DisplayName("GET /api/tareas/{id} devuelve una tarea")
        void buscar() throws Exception {
            when(servicio.buscar(1L)).thenReturn(tareaEjemplo);

            mockMvc.perform(get("/api/tareas/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.titulo").value("Ejemplo"));
        }

        @Test
        @DisplayName("POST /api/tareas responde 201 Created")
        void crear() throws Exception {
            when(servicio.crear(any(), any(), any())).thenReturn(tareaEjemplo);

            mockMvc.perform(post("/api/tareas")
                            .contentType("application/json")
                            .content("{\"titulo\":\"Ejemplo\",\"prioridad\":\"ALTA\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.titulo").value("Ejemplo"));
        }

        @Test
        @DisplayName("POST /api/tareas/{id}/completar marca la tarea")
        void completar() throws Exception {
            tareaEjemplo.completar();
            when(servicio.completar(1L)).thenReturn(tareaEjemplo);

            mockMvc.perform(post("/api/tareas/1/completar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completada").value(true));
        }

        @Test
        @DisplayName("DELETE /api/tareas/{id} responde 204 No Content")
        void eliminar() throws Exception {
            mockMvc.perform(delete("/api/tareas/1")).andExpect(status().isNoContent());

            verify(servicio).eliminar(1L);
        }

        @Test
        @DisplayName("GET /api/tareas/resumen devuelve los agregados")
        void resumen() throws Exception {
            when(servicio.resumen()).thenReturn(ResumenTareas.calcular(4, 1));

            mockMvc.perform(get("/api/tareas/resumen"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.porcentajeAvance").value(25));
        }

        @Test
        @DisplayName("Una regla de negocio violada se traduce en 400 Bad Request")
        void reglaVioladaDevuelve400() throws Exception {
            when(servicio.crear(any(), any(), any()))
                    .thenThrow(new TareaInvalidaException("El título de la tarea es obligatorio"));

            mockMvc.perform(post("/api/tareas")
                            .contentType("application/json")
                            .content("{\"titulo\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("El título de la tarea es obligatorio"));
        }

        @Test
        @DisplayName("Una tarea inexistente se traduce en 404 Not Found")
        void inexistenteDevuelve404() throws Exception {
            when(servicio.buscar(99L)).thenThrow(new TareaNoEncontradaException(99L));

            mockMvc.perform(get("/api/tareas/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("No existe la tarea con id 99"));
        }
    }

    @Nested
    @DisplayName("Interfaz web")
    class Web {

        private MockMvc mockMvc;

        @BeforeEach
        void montar() {
            InfoDespliegue info = new InfoDespliegue("1.0.0", "blue", "abc123", "2026-09-15");
            // Sin un resolutor de vistas, devolver la vista "tareas" desde
            // POST /tareas haría que MockMvc despache de vuelta a la misma URL.
            InternalResourceViewResolver resolutor = new InternalResourceViewResolver();
            resolutor.setPrefix("/templates/");
            resolutor.setSuffix(".html");

            mockMvc = MockMvcBuilders
                    .standaloneSetup(new TareaWebControlador(servicio, info))
                    .setViewResolvers(resolutor)
                    .build();
        }

        @Test
        @DisplayName("GET / entrega la vista con el listado y el resumen")
        void pintaVista() throws Exception {
            when(servicio.listar()).thenReturn(List.of(tareaEjemplo));
            when(servicio.resumen()).thenReturn(ResumenTareas.calcular(1, 0));

            mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("tareas"))
                    .andExpect(model().attributeExists("tareas", "resumen", "prioridades", "info"));
        }

        @Test
        @DisplayName("Crear con datos válidos redirige al listado")
        void crearRedirige() throws Exception {
            when(servicio.crear(eq("Nueva"), any(), any())).thenReturn(tareaEjemplo);

            mockMvc.perform(post("/tareas").param("titulo", "Nueva"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(view().name("redirect:/"));
        }

        @Test
        @DisplayName("Crear con datos inválidos vuelve a la vista con el mensaje de error")
        void crearInvalidoMuestraError() throws Exception {
            when(servicio.crear(any(), any(), any()))
                    .thenThrow(new TareaInvalidaException("El título de la tarea es obligatorio"));
            when(servicio.listar()).thenReturn(List.of());
            when(servicio.resumen()).thenReturn(ResumenTareas.calcular(0, 0));

            mockMvc.perform(post("/tareas").param("titulo", ""))
                    .andExpect(status().isOk())
                    .andExpect(view().name("tareas"))
                    .andExpect(model().attribute("mensajeError", "El título de la tarea es obligatorio"));
        }

        @Test
        @DisplayName("Completar y eliminar desde la web redirigen al listado")
        void completarYEliminarRedirigen() throws Exception {
            when(servicio.completar(1L)).thenReturn(tareaEjemplo);

            mockMvc.perform(post("/tareas/1/completar")).andExpect(view().name("redirect:/"));
            mockMvc.perform(post("/tareas/1/eliminar")).andExpect(view().name("redirect:/"));

            verify(servicio).eliminar(1L);
        }
    }

    @Nested
    @DisplayName("Endpoint de versión")
    class Version {

        @Test
        @DisplayName("Expone los metadatos de auditoría del despliegue")
        void exponeMetadatos() throws Exception {
            InfoDespliegue info = new InfoDespliegue("1.0.0", "green", "abc123", "2026-09-15T10:00:00Z");
            MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new VersionControlador(info)).build();

            mockMvc.perform(get("/api/version"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slot").value("green"))
                    .andExpect(jsonPath("$.version").value("1.0.0"))
                    .andExpect(jsonPath("$.commitSha").value("abc123"));

            assertThat(info.getFechaBuild()).isEqualTo("2026-09-15T10:00:00Z");
        }
    }
}
