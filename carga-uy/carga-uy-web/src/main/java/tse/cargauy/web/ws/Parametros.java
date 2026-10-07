package tse.cargauy.web.ws;

import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

public final class Parametros {

    private Parametros() {
    }

    public static <E extends Enum<E>> E enumeracion(Class<E> tipo, String nombre, String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new CargaUYException(CodigoError.PARAMETRO_INVALIDO, nombre, valor);
        }
    }
}
