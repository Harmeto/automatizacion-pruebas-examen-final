package cl.iplacex.automatizacion.gestortareas.aceptacion;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Runner del Acceptance Test Gate.
 *
 * <p>Ejecuta los escenarios BDD en Gherkin contra la aplicación ya desplegada
 * en el ambiente de prueba. Deliberadamente NO corre en la fase {@code verify}
 * habitual: el {@code pom.xml} lo excluye de Failsafe y solo lo habilita con
 * el perfil {@code -Pacceptance}, porque necesita un despliegue vivo contra el
 * cual apuntar.
 *
 * <pre>
 *   ./mvnw verify -Pacceptance -Dacceptance.url=http://localhost:8091
 * </pre>
 *
 * <p>El reporte HTML que produce es la evidencia que se publica al cerrar
 * el gate.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(
        key = GLUE_PROPERTY_NAME,
        value = "cl.iplacex.automatizacion.gestortareas.aceptacion.pasos")
@ConfigurationParameter(
        key = PLUGIN_PROPERTY_NAME,
        value = "pretty,"
                + "html:target/cucumber-reports/acceptance.html,"
                + "json:target/cucumber-reports/acceptance.json,"
                + "junit:target/cucumber-reports/acceptance.xml")
public class RunAcceptanceIT {
    // Clase sin cuerpo: la configuración vive en las anotaciones.
}
