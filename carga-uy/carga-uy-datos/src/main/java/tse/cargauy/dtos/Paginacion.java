package tse.cargauy.dtos;

import java.io.Serializable;

import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

public class Paginacion implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int TAMANIO_MAXIMO = 100;

    private final int pagina;
    private final int tamanio;

    public Paginacion(int pagina, int tamanio) {
        if (pagina < 0 || tamanio < 1 || tamanio > TAMANIO_MAXIMO) {
            throw new CargaUYException(CodigoError.PAGINACION_INVALIDA, String.valueOf(TAMANIO_MAXIMO));
        }
        this.pagina = pagina;
        this.tamanio = tamanio;
    }

    public int getPagina() {
        return pagina;
    }

    public int getTamanio() {
        return tamanio;
    }

    public int getDesde() {
        return pagina * tamanio;
    }
}
