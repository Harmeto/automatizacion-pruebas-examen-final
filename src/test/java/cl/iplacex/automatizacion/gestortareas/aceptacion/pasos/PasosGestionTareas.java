package cl.iplacex.automatizacion.gestortareas.aceptacion.pasos;

import static org.assertj.core.api.Assertions.assertThat;

import cl.iplacex.automatizacion.gestortareas.aceptacion.paginas.PaginaTareas;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Traducción de los pasos Gherkin a acciones sobre el sistema.
 *
 * <p>Se mantienen deliberadamente delgados: toda la mecánica de la interfaz
 * vive en el Page Object {@link PaginaTareas}. Es la práctica de mantener los step
 * definitions limpios y enfocados en el qué más que en el cómo.
 */
public class PasosGestionTareas {

    private PaginaTareas pagina;
    private List<String> titulosDeEjemplo;

    @Dado("que la aplicación está desplegada y disponible")
    public void laAplicacionEstaDesplegada() throws Exception {
        HttpClient cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpResponse<String> respuesta = cliente.send(
                HttpRequest.newBuilder(URI.create(ContextoNavegador.URL_BASE + "/api/version"))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(respuesta.statusCode())
                .as("El ambiente %s debe responder antes de correr el gate",
                        ContextoNavegador.URL_BASE)
                .isEqualTo(200);
    }

    @Cuando("abro el gestor de tareas")
    public void abroElGestor() {
        pagina = new PaginaTareas(ContextoNavegador.navegador(), ContextoNavegador.URL_BASE);
        pagina.abrir();
    }

    @Y("registro la tarea {string} con prioridad {string}")
    public void registroLaTarea(String titulo, String prioridad) {
        pagina.crearTarea(titulo, prioridad);
    }

    @Y("registro {int} tareas de ejemplo")
    public void registroTareasDeEjemplo(int cantidad) {
        titulosDeEjemplo = java.util.stream.IntStream.rangeClosed(1, cantidad)
                .mapToObj(numero -> "Tarea de ejemplo " + numero)
                .toList();

        titulosDeEjemplo.forEach(titulo -> pagina.crearTarea(titulo, "MEDIA"));
    }

    @Y("completo las primeras {int} tareas")
    public void completoLasPrimeras(int cantidad) {
        titulosDeEjemplo.stream().limit(cantidad).forEach(pagina::completar);
    }

    @Y("marco como completada la tarea {string}")
    public void marcoComoCompletada(String titulo) {
        pagina.completar(titulo);
    }

    @Y("elimino la tarea {string}")
    public void eliminoLaTarea(String titulo) {
        pagina.eliminar(titulo);
    }

    @Entonces("la tarea {string} aparece en el listado")
    public void laTareaAparece(String titulo) {
        assertThat(pagina.muestraTarea(titulo))
                .as("Se esperaba ver la tarea '%s'. Visibles: %s", titulo, pagina.titulosVisibles())
                .isTrue();
    }

    @Entonces("la tarea {string} ya no aparece en el listado")
    public void laTareaYaNoAparece(String titulo) {
        assertThat(pagina.muestraTarea(titulo))
                .as("La tarea '%s' debió desaparecer del listado", titulo)
                .isFalse();
    }

    @Entonces("la tarea {string} figura como {string}")
    public void laTareaFiguraComo(String titulo, String estadoEsperado) {
        assertThat(pagina.estadoDe(titulo)).isEqualTo(estadoEsperado);
    }

    @Entonces("el resumen indica {int} tareas totales")
    public void elResumenIndicaTotales(int esperado) {
        assertThat(pagina.totalMostrado()).isEqualTo(esperado);
    }

    @Entonces("el avance mostrado es {int} por ciento")
    public void elAvanceMostradoEs(int esperado) {
        assertThat(pagina.avanceMostrado())
                .as("Porcentaje de avance mostrado en el tablero")
                .isEqualTo(esperado);
    }

    @Entonces("se muestra el mensaje de error {string}")
    public void seMuestraElMensajeDeError(String fragmento) {
        assertThat(pagina.mensajeDeError())
                .as("Mensaje de error visible en pantalla")
                .contains(fragmento);
    }

    @Entonces("la página muestra el identificador del slot desplegado")
    public void laPaginaMuestraElSlot() {
        assertThat(pagina.slotDesplegado()).isNotBlank();
        assertThat(pagina.versionDesplegada()).isNotBlank();
    }
}
