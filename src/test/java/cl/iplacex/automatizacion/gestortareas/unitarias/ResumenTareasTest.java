package cl.iplacex.automatizacion.gestortareas.unitarias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.iplacex.automatizacion.gestortareas.modelo.ResumenTareas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Pruebas unitarias del cálculo de avance.
 *
 * <p>Es lógica pura, sin dependencias ni estado: se ejecuta en milisegundos y
 * nunca es intermitente. Es el tipo de prueba que ME_4-3 §3.1 sitúa en la base
 * de la estrategia y que corre en la etapa de commit del pipeline.
 */
@DisplayName("Cálculo del resumen de tareas")
class ResumenTareasTest {

    @Test
    @DisplayName("Un tablero vacío reporta 0 % de avance en lugar de dividir por cero")
    void tableroVacioNoDivideporCero() {
        ResumenTareas resumen = ResumenTareas.calcular(0, 0);

        assertThat(resumen.total()).isZero();
        assertThat(resumen.pendientes()).isZero();
        assertThat(resumen.porcentajeAvance()).isZero();
    }

    @Test
    @DisplayName("Las pendientes son la diferencia entre el total y las completadas")
    void calculaPendientes() {
        ResumenTareas resumen = ResumenTareas.calcular(10, 4);

        assertThat(resumen.pendientes()).isEqualTo(6);
        assertThat(resumen.completadas()).isEqualTo(4);
    }

    /**
     * Datos parametrizados: cubren varios escenarios con una sola lógica de
     * prueba, práctica recomendada en ME_3-3 §3.2.
     */
    @ParameterizedTest(name = "{0} tareas con {1} completadas => {2} % de avance")
    @CsvSource({
            "1,  0,   0",
            "1,  1, 100",
            "4,  1,  25",
            "4,  2,  50",
            "3,  1,  33",
            "3,  2,  67",
            "7,  5,  71"
    })
    @DisplayName("El porcentaje se redondea al entero más cercano")
    void calculaPorcentajeRedondeado(long total, long completadas, int esperado) {
        assertThat(ResumenTareas.calcular(total, completadas).porcentajeAvance()).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Rechaza conteos negativos")
    void rechazaNegativos() {
        assertThatThrownBy(() -> ResumenTareas.calcular(-1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativos");
    }

    @Test
    @DisplayName("Rechaza más completadas que el total, que sería un estado imposible")
    void rechazaCompletadasMayoresAlTotal() {
        assertThatThrownBy(() -> ResumenTareas.calcular(2, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pueden superar");
    }
}
