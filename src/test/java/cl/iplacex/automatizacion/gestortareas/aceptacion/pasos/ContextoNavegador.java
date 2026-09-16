package cl.iplacex.automatizacion.gestortareas.aceptacion.pasos;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Ciclo de vida del navegador y del estado de datos para los escenarios BDD.
 *
 * <p>Los hooks {@code @Before} y {@code @After} implementan la restauración
 * del entorno esperada de una suite: cada escenario arranca contra un tablero
 * vacío y cierra su propio navegador. Gracias a eso la suite es idempotente y
 * puede repetirse sin arrastrar datos de la ejecución anterior.
 */
public class ContextoNavegador {

    /** URL del ambiente desplegado. La inyecta el pipeline al cerrar el gate. */
    static final String URL_BASE =
            System.getProperty("acceptance.url", "http://localhost:8090");

    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static WebDriver navegador;

    static WebDriver navegador() {
        return navegador;
    }

    @Before
    public void abrirNavegador() {
        ChromeOptions opciones = new ChromeOptions();
        // Headless: el gate corre igual en un agente de CI sin escritorio.
        opciones.addArguments(
                "--headless=new",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--window-size=1280,900");

        // Selenium Manager resuelve el driver de Chrome automáticamente,
        // evitando versionar binarios en el repositorio.
        navegador = new ChromeDriver(opciones);

        limpiarTareasExistentes();
    }

    @After
    public void cerrarNavegador(Scenario escenario) {
        if (navegador == null) {
            return;
        }
        try {
            // Captura de pantalla ante fallo: es la evidencia contextual que
            // se adjunta al reporte del gate.
            if (escenario.isFailed() && navegador instanceof TakesScreenshot capturador) {
                escenario.attach(
                        capturador.getScreenshotAs(OutputType.BYTES),
                        "image/png",
                        "pantalla-al-fallar");
            }
        } finally {
            navegador.quit();
            navegador = null;
        }
    }

    /**
     * Deja el tablero vacío usando la propia API pública de la aplicación.
     *
     * <p>Se usa la API y no un acceso directo a la base porque el gate prueba
     * el sistema desplegado como una caja negra: no tiene ni debe tener
     * credenciales de su base de datos.
     */
    private void limpiarTareasExistentes() {
        try {
            HttpResponse<String> listado = CLIENTE.send(
                    HttpRequest.newBuilder(URI.create(URL_BASE + "/api/tareas")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            for (String id : extraerIdentificadores(listado.body())) {
                CLIENTE.send(
                        HttpRequest.newBuilder(URI.create(URL_BASE + "/api/tareas/" + id))
                                .DELETE().build(),
                        HttpResponse.BodyHandlers.discarding());
            }
        } catch (IOException | InterruptedException fallo) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "No se pudo preparar el estado inicial contra " + URL_BASE, fallo);
        }
    }

    /** Extrae los "id" del JSON del listado sin añadir una librería solo para esto. */
    private List<String> extraerIdentificadores(String json) {
        return java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)")
                .matcher(json)
                .results()
                .map(coincidencia -> coincidencia.group(1))
                .toList();
    }
}
