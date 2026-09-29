package tse.cargauy.exceptions;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class CargaUYException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final CodigoError codigo;
    private final String[] parametros;

    public CargaUYException(CodigoError codigo, String... parametros) {
        super(codigo.name());
        this.codigo = codigo;
        this.parametros = parametros;
    }

    public CodigoError getCodigo() {
        return codigo;
    }

    public String[] getParametros() {
        return parametros;
    }
}
