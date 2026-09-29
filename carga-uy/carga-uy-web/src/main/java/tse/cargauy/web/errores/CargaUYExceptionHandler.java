package tse.cargauy.web.errores;

import java.util.Iterator;

import jakarta.faces.FacesException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerWrapper;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ExceptionQueuedEvent;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.MensajesError;

public class CargaUYExceptionHandler extends ExceptionHandlerWrapper {

    public CargaUYExceptionHandler(ExceptionHandler wrapped) {
        super(wrapped);
    }

    @Override
    public void handle() throws FacesException {
        Iterator<ExceptionQueuedEvent> eventos = getUnhandledExceptionQueuedEvents().iterator();
        while (eventos.hasNext()) {
            CargaUYException excepcion = buscarCargaUY(eventos.next().getContext().getException());
            if (excepcion == null) {
                continue;
            }

            FacesContext contexto = FacesContext.getCurrentInstance();
            contexto.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, MensajesError.resolver(excepcion), null));
            contexto.validationFailed();
            contexto.renderResponse();
            eventos.remove();
        }

        getWrapped().handle();
    }

    private CargaUYException buscarCargaUY(Throwable causa) {
        while (causa != null) {
            if (causa instanceof CargaUYException cargaUYException) {
                return cargaUYException;
            }
            causa = causa.getCause();
        }
        return null;
    }
}
