package cl.iplacex.automatizacion.gestortareas.integracion;

import static org.assertj.core.api.Assertions.assertThat;

import cl.iplacex.automatizacion.gestortareas.modelo.Prioridad;
import cl.iplacex.automatizacion.gestortareas.repositorio.TareaRepositorio;
import cl.iplacex.automatizacion.gestortareas.servicio.TareaServicio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Pruebas de integración de la interfaz web renderizada.
 *
 * <p>Verifican que Thymeleaf produzca HTML con los identificadores que los
 * Page Objects de Selenium esperan encontrar. Detectar aquí una plantilla rota
 * es mucho más barato que descubrirlo en el Acceptance Gate, donde además hay
 * que levantar un navegador.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Integración: renderizado de la interfaz web")
class InterfazWebIT {

    @Autowired
    private TestRestTemplate cliente;

    @Autowired
    private TareaServicio servicio;

    @Autowired
    private TareaRepositorio repositorio;

    @BeforeEach
    void prepararEstadoConocido() {
        repositorio.deleteAll();
    }

    @Test
    @DisplayName("La página vacía informa que no hay tareas registradas")
    void paginaVaciaMuestraMensaje() {
        String html = cliente.getForObject("/", String.class);

        assertThat(html).contains("id=\"sin-tareas\"");
        assertThat(html).doesNotContain("id=\"tabla-tareas\"");
    }

    @Test
    @DisplayName("La página lista las tareas con los identificadores que usa Selenium")
    void paginaListaTareasConIdentificadores() {
        servicio.crear("Revisar el pipeline", "stage de build", Prioridad.ALTA);

        String html = cliente.getForObject("/", String.class);

        assertThat(html).contains("id=\"tabla-tareas\"");
        assertThat(html).contains("Revisar el pipeline");
        assertThat(html).contains("id=\"resumen-total\"");
        assertThat(html).contains("id=\"resumen-pendientes\"");
        assertThat(html).contains("id=\"app-slot\"");
    }

    @Test
    @DisplayName("El formulario web crea la tarea y el listado resultante ya la muestra")
    void formularioCreaTarea() {
        // El cliente sigue la redirección automáticamente, de modo que la
        // respuesta final es ya el listado: se verifica el efecto observable
        // (la tarea persistida y visible) en vez del código intermedio.
        ResponseEntity<String> respuesta = enviarFormulario("Tarea desde el formulario");

        assertThat(respuesta.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(respuesta.getBody()).contains("Tarea desde el formulario");
        assertThat(repositorio.findByTituloIgnoreCase("Tarea desde el formulario")).isPresent();
    }

    @Test
    @DisplayName("El formulario con título vacío muestra el error sin crear nada")
    void formularioInvalidoMuestraError() {
        ResponseEntity<String> respuesta = enviarFormulario("");

        assertThat(respuesta.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(respuesta.getBody()).contains("id=\"mensaje-error\"");
        assertThat(respuesta.getBody()).contains("obligatorio");
        assertThat(repositorio.count()).isZero();
    }

    private ResponseEntity<String> enviarFormulario(String titulo) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> campos = new LinkedMultiValueMap<>();
        campos.add("titulo", titulo);
        campos.add("descripcion", "creada en prueba de integración");
        campos.add("prioridad", "MEDIA");

        return cliente.exchange("/tareas", HttpMethod.POST,
                new HttpEntity<>(campos, cabeceras), String.class);
    }
}
