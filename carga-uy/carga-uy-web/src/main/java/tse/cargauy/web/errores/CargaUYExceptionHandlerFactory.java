package tse.cargauy.web.errores;

import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerFactory;

public class CargaUYExceptionHandlerFactory extends ExceptionHandlerFactory {

    public CargaUYExceptionHandlerFactory(ExceptionHandlerFactory wrapped) {
        super(wrapped);
    }

    @Override
    public ExceptionHandler getExceptionHandler() {
        return new CargaUYExceptionHandler(getWrapped().getExceptionHandler());
    }
}
