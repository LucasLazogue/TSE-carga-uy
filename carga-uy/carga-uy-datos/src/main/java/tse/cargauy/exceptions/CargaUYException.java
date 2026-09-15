package tse.cargauy.exceptions;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class CargaUYException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CargaUYException(String mensaje) {
        super(mensaje);
    }
}
