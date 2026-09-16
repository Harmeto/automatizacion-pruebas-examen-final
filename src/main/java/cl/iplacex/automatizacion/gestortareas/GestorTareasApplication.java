package cl.iplacex.automatizacion.gestortareas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicación bajo prueba.
 *
 * <p>Esta aplicación no es el objetivo del examen: es el sistema sobre el cual
 * se demuestra la estrategia de automatización. Se mantuvo deliberadamente
 * pequeña pero completa (API REST + interfaz web + persistencia) para que los
 * tres niveles de prueba tengan algo real que verificar.
 */
@SpringBootApplication
public class GestorTareasApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestorTareasApplication.class, args);
    }
}
