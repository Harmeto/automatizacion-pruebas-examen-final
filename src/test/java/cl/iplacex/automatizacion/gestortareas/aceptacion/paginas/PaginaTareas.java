package cl.iplacex.automatizacion.gestortareas.aceptacion.paginas;

import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object de la pantalla principal del gestor de tareas.
 *
 * <p>Concentra todo el conocimiento sobre CÓMO se interactúa con la interfaz
 * (selectores, esperas, formularios). Los step definitions quedan así
 * expresando solo el QUÉ, que es la separación de responsabilidades propia
 * del patrón Page Object.
 *
 * <p>Los elementos se localizan por identificador estable o por el texto del
 * título de la tarea, nunca por posición: así un cambio de maquetación no
 * rompe la suite.
 */
public class PaginaTareas {

    private final WebDriver navegador;
    private final WebDriverWait espera;
    private final String urlBase;

    public PaginaTareas(WebDriver navegador, String urlBase) {
        this.navegador = navegador;
        this.urlBase = urlBase;
        // Esperas explícitas en lugar de pausas fijas: es la forma recomendada
        // de evitar pruebas intermitentes (flaky).
        this.espera = new WebDriverWait(navegador, Duration.ofSeconds(10));
    }

    public void abrir() {
        navegador.get(urlBase + "/");
        esperarPaginaLista();
    }

    /** Completa y envía el formulario de alta, esperando la recarga resultante. */
    public void crearTarea(String titulo, String prioridad) {
        WebElement campoTitulo = espera.until(
                ExpectedConditions.elementToBeClickable(By.id("titulo")));
        campoTitulo.clear();
        campoTitulo.sendKeys(titulo);

        WebElement campoDescripcion = navegador.findElement(By.id("descripcion"));
        campoDescripcion.clear();
        campoDescripcion.sendKeys("creada por el acceptance test");

        new Select(navegador.findElement(By.id("prioridad"))).selectByValue(prioridad);

        enviarYEsperarRecarga(navegador.findElement(By.id("btn-crear")));
    }

    public boolean muestraTarea(String titulo) {
        return !buscarFilas(titulo).isEmpty();
    }

    /** Estado textual de una tarea: "Pendiente" o "Completada". */
    public String estadoDe(String titulo) {
        return filaDe(titulo).findElement(By.className("celda-estado")).getText().trim();
    }

    public void completar(String titulo) {
        enviarYEsperarRecarga(filaDe(titulo).findElement(By.cssSelector("button[id^='completar-']")));
    }

    public void eliminar(String titulo) {
        enviarYEsperarRecarga(filaDe(titulo).findElement(By.cssSelector("button[id^='eliminar-']")));
    }

    public int totalMostrado() {
        return Integer.parseInt(textoDe("resumen-total"));
    }

    public int avanceMostrado() {
        // El elemento muestra "40 %": se extrae solo la parte numérica.
        return Integer.parseInt(textoDe("resumen-avance").replace("%", "").trim());
    }

    public String slotDesplegado() {
        return textoDe("app-slot");
    }

    public String versionDesplegada() {
        return textoDe("app-version");
    }

    public String mensajeDeError() {
        try {
            return navegador.findElement(By.id("mensaje-error")).getText();
        } catch (NoSuchElementException ausente) {
            return "";
        }
    }

    /** Títulos de todas las tareas visibles, en el orden en que se muestran. */
    public List<String> titulosVisibles() {
        return navegador.findElements(By.className("celda-titulo")).stream()
                .map(elemento -> elemento.getText().trim())
                .toList();
    }

    /**
     * Pulsa un control que provoca navegación y espera a que la página nueva
     * esté realmente cargada.
     *
     * <p>Esperar solo por la presencia del formulario no basta: ese elemento
     * también existe en la página anterior, de modo que la espera se satisface
     * antes de que el navegador reemplace el documento y las siguientes
     * interacciones fallan con "stale element reference". Se espera primero a
     * que el documento viejo quede obsoleto y recién después a que el nuevo
     * esté disponible.
     */
    private void enviarYEsperarRecarga(WebElement control) {
        WebElement documentoAnterior = navegador.findElement(By.tagName("html"));
        control.click();
        espera.until(ExpectedConditions.stalenessOf(documentoAnterior));
        esperarPaginaLista();
    }

    private void esperarPaginaLista() {
        espera.until(ExpectedConditions.elementToBeClickable(By.id("btn-crear")));
    }

    private String textoDe(String id) {
        return espera.until(ExpectedConditions.presenceOfElementLocated(By.id(id))).getText().trim();
    }

    private WebElement filaDe(String titulo) {
        List<WebElement> filas = buscarFilas(titulo);
        if (filas.isEmpty()) {
            throw new NoSuchElementException("No se encontró la tarea con título: " + titulo);
        }
        return filas.get(0);
    }

    private List<WebElement> buscarFilas(String titulo) {
        return navegador.findElements(By.xpath(
                "//tr[td[@class='celda-titulo'][normalize-space()=" + comillar(titulo) + "]]"));
    }

    /** Envuelve el texto para XPath, tolerando títulos que contengan comillas. */
    private String comillar(String texto) {
        if (!texto.contains("'")) {
            return "'" + texto + "'";
        }
        return "concat('" + texto.replace("'", "',\"'\",'") + "')";
    }
}
