package tse.cargauy.negocio.fiscalizacion;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import tse.cargauy.dtos.CasoFiscalizacionDto;
import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

public final class MaquinaEstadosCaso {

    public static final int LARGO_MAXIMO_TEXTO = 4000;

    private static final Map<EstadoCaso, Set<EstadoCaso>> TRANSICIONES = Map.of(
            EstadoCaso.DETECTADA, EnumSet.of(EstadoCaso.NOTIFICADA),
            EstadoCaso.NOTIFICADA, EnumSet.of(EstadoCaso.EN_DESCARGO, EstadoCaso.CONFIRMADA, EstadoCaso.DESESTIMADA),
            EstadoCaso.EN_DESCARGO, EnumSet.of(EstadoCaso.CONFIRMADA, EstadoCaso.DESESTIMADA),
            EstadoCaso.CONFIRMADA, EnumSet.noneOf(EstadoCaso.class),
            EstadoCaso.DESESTIMADA, EnumSet.noneOf(EstadoCaso.class));

    private MaquinaEstadosCaso() {
    }

    public static boolean permitida(EstadoCaso desde, EstadoCaso hacia) {
        return TRANSICIONES.get(desde).contains(hacia);
    }

    public static void validarNotificacion(CasoFiscalizacionDto caso) {
        validarTransicion(caso.getEstado(), EstadoCaso.NOTIFICADA);
    }

    public static void validarDescargo(CasoFiscalizacionDto caso, String texto, LocalDateTime ahora) {
        if (caso.getDescargo() != null) {
            throw new CargaUYException(CodigoError.DESCARGO_YA_PRESENTADO);
        }
        validarTransicion(caso.getEstado(), EstadoCaso.EN_DESCARGO);
        if (ahora.isAfter(caso.getVenceDescargo())) {
            throw new CargaUYException(CodigoError.DESCARGO_FUERA_DE_PLAZO,
                    caso.getVenceDescargo().format(CasoFiscalizacionDto.FORMATO_FECHA));
        }
        if (texto == null || texto.isBlank()) {
            throw new CargaUYException(CodigoError.DESCARGO_TEXTO_REQUERIDO);
        }
        validarLargo(texto);
    }

    public static void validarResolucion(CasoFiscalizacionDto caso, EstadoCaso resultado, String fundamento, LocalDateTime ahora) {
        if (resultado != EstadoCaso.CONFIRMADA && resultado != EstadoCaso.DESESTIMADA) {
            throw new CargaUYException(CodigoError.PARAMETRO_INVALIDO, "resultado", String.valueOf(resultado));
        }
        validarTransicion(caso.getEstado(), resultado);
        if (fundamento == null || fundamento.isBlank()) {
            throw new CargaUYException(CodigoError.CASO_FUNDAMENTO_REQUERIDO);
        }
        validarLargo(fundamento);
        if (caso.getEstado() == EstadoCaso.NOTIFICADA && !ahora.isAfter(caso.getVenceDescargo())) {
            throw new CargaUYException(CodigoError.CASO_PLAZO_NO_VENCIDO,
                    caso.getVenceDescargo().format(CasoFiscalizacionDto.FORMATO_FECHA));
        }
    }

    private static void validarTransicion(EstadoCaso desde, EstadoCaso hacia) {
        if (!permitida(desde, hacia)) {
            throw new CargaUYException(CodigoError.CASO_TRANSICION_INVALIDA, desde.name(), hacia.name());
        }
    }

    private static void validarLargo(String texto) {
        if (texto.strip().length() > LARGO_MAXIMO_TEXTO) {
            throw new CargaUYException(CodigoError.CASO_TEXTO_DEMASIADO_LARGO, String.valueOf(LARGO_MAXIMO_TEXTO));
        }
    }
}
