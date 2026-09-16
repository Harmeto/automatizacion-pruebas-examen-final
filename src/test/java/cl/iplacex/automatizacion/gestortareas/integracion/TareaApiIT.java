package cl.iplacex.automatizacion.gestortareas.integracion;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import cl.iplacex.automatizacion.gestortareas.repositorio.TareaRepositorio;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Pruebas de integración de la API REST.
 *
 * <p>A diferencia de las unitarias, estas levantan el contexto completo de
 * Spring, un servidor HTTP en un puerto real y la base de datos H2. Verifican
 * la interacción entre controlador, servicio, repositorio y persistencia:
 * el caso "API + base de datos" que describe ME_4-3 §3.1.
 *
 * <p><b>Idempotencia:</b> cada prueba limpia el repositorio antes de ejecutarse
 * (ME_3-3 §3.2), de modo que la suite puede repetirse indefinidamente y en
 * cualquier orden sin arrastrar estado entre casos.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Integración: API REST sobre base de datos real")
class TareaApiIT {

    @LocalServerPort
    private int puerto;

    @Autowired
    private TareaRepositorio repositorio;

    @BeforeEach
    void prepararEstadoConocido() {
        RestAssured.port = puerto;
        // Restauración del entorno antes de cada escenario: garantiza que el
        // resultado no dependa de la ejecución anterior.
        repositorio.deleteAll();
    }

    @Test
    @DisplayName("Una tarea creada por la API queda efectivamente persistida")
    void creaYPersiste() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"titulo\":\"Configurar Jenkins\",\"descripcion\":\"stage de build\",\"prioridad\":\"ALTA\"}")
        .when()
            .post("/api/tareas")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("titulo", equalTo("Configurar Jenkins"))
            .body("completada", equalTo(false));

        // La verificación clave: el dato viajó hasta la base de datos.
        org.assertj.core.api.Assertions.assertThat(repositorio.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("El listado devuelve las tareas efectivamente almacenadas")
    void listaDesdeLaBaseDeDatos() {
        crearTarea("Primera");
        crearTarea("Segunda");

        given()
        .when()
            .get("/api/tareas")
        .then()
            .statusCode(200)
            .body("$", hasSize(2));
    }

    @Test
    @DisplayName("La restricción de título único se aplica contra la base de datos")
    void rechazaDuplicadoContraLaBase() {
        crearTarea("Tarea repetida");

        given()
            .contentType(ContentType.JSON)
            .body("{\"titulo\":\"TAREA REPETIDA\",\"prioridad\":\"BAJA\"}")
        .when()
            .post("/api/tareas")
        .then()
            .statusCode(400)
            .body("error", org.hamcrest.Matchers.containsString("Ya existe"));

        org.assertj.core.api.Assertions.assertThat(repositorio.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Completar una tarea persiste el cambio de estado")
    void completarPersisteElEstado() {
        int id = crearTarea("Desplegar a staging");

        given()
        .when()
            .post("/api/tareas/" + id + "/completar")
        .then()
            .statusCode(200)
            .body("completada", equalTo(true));

        // Se relee desde la base para confirmar que el cambio no quedó solo en memoria.
        org.assertj.core.api.Assertions
                .assertThat(repositorio.findById((long) id).orElseThrow().isCompletada())
                .isTrue();
    }

    @Test
    @DisplayName("Eliminar una tarea la borra de la base de datos")
    void eliminarBorraDeLaBase() {
        int id = crearTarea("Tarea temporal");

        given().when().delete("/api/tareas/" + id).then().statusCode(204);

        org.assertj.core.api.Assertions.assertThat(repositorio.existsById((long) id)).isFalse();
    }

    @Test
    @DisplayName("El resumen agrega correctamente sobre los datos reales")
    void resumenSobreDatosReales() {
        int primera = crearTarea("Una");
        crearTarea("Dos");
        crearTarea("Tres");
        crearTarea("Cuatro");

        given().when().post("/api/tareas/" + primera + "/completar").then().statusCode(200);

        given()
        .when()
            .get("/api/tareas/resumen")
        .then()
            .statusCode(200)
            .body("total", equalTo(4))
            .body("completadas", equalTo(1))
            .body("pendientes", equalTo(3))
            .body("porcentajeAvance", equalTo(25));
    }

    @Test
    @DisplayName("Consultar una tarea inexistente devuelve 404")
    void inexistenteDevuelve404() {
        given().when().get("/api/tareas/999999").then().statusCode(404);
    }

    @Test
    @DisplayName("El endpoint de versión responde con los metadatos del despliegue")
    void exponeMetadatosDeDespliegue() {
        given()
        .when()
            .get("/api/version")
        .then()
            .statusCode(200)
            .body("version", notNullValue())
            .body("slot", notNullValue());
    }

    /** Crea una tarea por la API y devuelve su id. */
    private int crearTarea(String titulo) {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"titulo\":\"" + titulo + "\",\"prioridad\":\"MEDIA\"}")
            .when()
                .post("/api/tareas")
            .then()
                .statusCode(201)
                .extract().path("id");
    }
}
