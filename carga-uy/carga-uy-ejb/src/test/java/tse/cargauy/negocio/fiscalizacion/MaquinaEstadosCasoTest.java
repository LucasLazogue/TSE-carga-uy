package tse.cargauy.negocio.fiscalizacion;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import tse.cargauy.dtos.CasoFiscalizacionDto;
import tse.cargauy.dtos.DescargoDto;
import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

class MaquinaEstadosCasoTest {

    private static final LocalDateTime VENCE = LocalDateTime.of(2026, 10, 20, 12, 0);
    private static final LocalDateTime EN_PLAZO = VENCE.minusDays(1);
    private static final LocalDateTime VENCIDO = VENCE.plusMinutes(1);

    private static CasoFiscalizacionDto caso(EstadoCaso estado) {
        CasoFiscalizacionDto caso = new CasoFiscalizacionDto();
        caso.setEstado(estado);
        caso.setVenceDescargo(VENCE);
        return caso;
    }

    private static void rechaza(CodigoError codigo, Executable accion) {
        assertEquals(codigo, assertThrows(CargaUYException.class, accion).getCodigo());
    }

    @ParameterizedTest
    @CsvSource({
        "DETECTADA,DETECTADA,false", "DETECTADA,NOTIFICADA,true", "DETECTADA,EN_DESCARGO,false",
        "DETECTADA,CONFIRMADA,false", "DETECTADA,DESESTIMADA,false",
        "NOTIFICADA,DETECTADA,false", "NOTIFICADA,NOTIFICADA,false", "NOTIFICADA,EN_DESCARGO,true",
        "NOTIFICADA,CONFIRMADA,true", "NOTIFICADA,DESESTIMADA,true",
        "EN_DESCARGO,DETECTADA,false", "EN_DESCARGO,NOTIFICADA,false", "EN_DESCARGO,EN_DESCARGO,false",
        "EN_DESCARGO,CONFIRMADA,true", "EN_DESCARGO,DESESTIMADA,true",
        "CONFIRMADA,DETECTADA,false", "CONFIRMADA,NOTIFICADA,false", "CONFIRMADA,EN_DESCARGO,false",
        "CONFIRMADA,CONFIRMADA,false", "CONFIRMADA,DESESTIMADA,false",
        "DESESTIMADA,DETECTADA,false", "DESESTIMADA,NOTIFICADA,false", "DESESTIMADA,EN_DESCARGO,false",
        "DESESTIMADA,CONFIRMADA,false", "DESESTIMADA,DESESTIMADA,false"
    })
    void soloLasTransicionesPrevistasEstanPermitidas(EstadoCaso desde, EstadoCaso hacia, boolean permitida) {
        assertEquals(permitida, MaquinaEstadosCaso.permitida(desde, hacia));
    }

    @Test
    void soloSeNotificaUnCasoDetectado() {
        assertDoesNotThrow(() -> MaquinaEstadosCaso.validarNotificacion(caso(EstadoCaso.DETECTADA)));
        rechaza(CodigoError.CASO_TRANSICION_INVALIDA, () -> MaquinaEstadosCaso.validarNotificacion(caso(EstadoCaso.NOTIFICADA)));
    }

    @Test
    void elDescargoSeAceptaDentroDelPlazo() {
        assertDoesNotThrow(() -> MaquinaEstadosCaso.validarDescargo(caso(EstadoCaso.NOTIFICADA), "texto", EN_PLAZO));
        assertDoesNotThrow(() -> MaquinaEstadosCaso.validarDescargo(caso(EstadoCaso.NOTIFICADA), "texto", VENCE));
    }

    @Test
    void elDescargoFueraDePlazoSeRechaza() {
        rechaza(CodigoError.DESCARGO_FUERA_DE_PLAZO,
                () -> MaquinaEstadosCaso.validarDescargo(caso(EstadoCaso.NOTIFICADA), "texto", VENCIDO));
    }

    @Test
    void soloSeAceptaUnDescargo() {
        CasoFiscalizacionDto caso = caso(EstadoCaso.EN_DESCARGO);
        caso.setDescargo(new DescargoDto("anterior"));

        rechaza(CodigoError.DESCARGO_YA_PRESENTADO, () -> MaquinaEstadosCaso.validarDescargo(caso, "otro", EN_PLAZO));
    }

    @Test
    void elDescargoNecesitaTexto() {
        rechaza(CodigoError.DESCARGO_TEXTO_REQUERIDO, () -> MaquinaEstadosCaso.validarDescargo(caso(EstadoCaso.NOTIFICADA), "  ", EN_PLAZO));
        rechaza(CodigoError.CASO_TEXTO_DEMASIADO_LARGO, () -> MaquinaEstadosCaso.validarDescargo(caso(EstadoCaso.NOTIFICADA),
                "x".repeat(MaquinaEstadosCaso.LARGO_MAXIMO_TEXTO + 1), EN_PLAZO));
    }

    @Test
    void noSePresentaDescargoEnUnCasoResuelto() {
        rechaza(CodigoError.CASO_TRANSICION_INVALIDA, () -> MaquinaEstadosCaso.validarDescargo(caso(EstadoCaso.CONFIRMADA), "texto", EN_PLAZO));
    }

    @Test
    void sinDescargoSoloSeResuelveConElPlazoVencido() {
        rechaza(CodigoError.CASO_PLAZO_NO_VENCIDO, () -> MaquinaEstadosCaso.validarResolucion(caso(EstadoCaso.NOTIFICADA),
                EstadoCaso.CONFIRMADA, "fundamento", EN_PLAZO));
        assertDoesNotThrow(() -> MaquinaEstadosCaso.validarResolucion(caso(EstadoCaso.NOTIFICADA), EstadoCaso.DESESTIMADA,
                "fundamento", VENCIDO));
    }

    @Test
    void conDescargoSeResuelveSinEsperarElPlazo() {
        assertDoesNotThrow(() -> MaquinaEstadosCaso.validarResolucion(caso(EstadoCaso.EN_DESCARGO), EstadoCaso.CONFIRMADA,
                "fundamento", EN_PLAZO));
    }

    @Test
    void laResolucionNecesitaFundamento() {
        rechaza(CodigoError.CASO_FUNDAMENTO_REQUERIDO, () -> MaquinaEstadosCaso.validarResolucion(caso(EstadoCaso.EN_DESCARGO),
                EstadoCaso.CONFIRMADA, null, EN_PLAZO));
        rechaza(CodigoError.CASO_FUNDAMENTO_REQUERIDO, () -> MaquinaEstadosCaso.validarResolucion(caso(EstadoCaso.EN_DESCARGO),
                EstadoCaso.DESESTIMADA, " ", EN_PLAZO));
    }

    @Test
    void elResultadoEsConfirmadaODesestimada() {
        rechaza(CodigoError.PARAMETRO_INVALIDO, () -> MaquinaEstadosCaso.validarResolucion(caso(EstadoCaso.NOTIFICADA),
                EstadoCaso.EN_DESCARGO, "fundamento", VENCIDO));
        rechaza(CodigoError.PARAMETRO_INVALIDO, () -> MaquinaEstadosCaso.validarResolucion(caso(EstadoCaso.NOTIFICADA),
                null, "fundamento", VENCIDO));
    }

    @Test
    void unCasoResueltoNoSeVuelveAResolver() {
        rechaza(CodigoError.CASO_TRANSICION_INVALIDA, () -> MaquinaEstadosCaso.validarResolucion(caso(EstadoCaso.CONFIRMADA),
                EstadoCaso.DESESTIMADA, "fundamento", VENCIDO));
        rechaza(CodigoError.CASO_TRANSICION_INVALIDA, () -> MaquinaEstadosCaso.validarResolucion(caso(EstadoCaso.DETECTADA),
                EstadoCaso.CONFIRMADA, "fundamento", VENCIDO));
    }
}
