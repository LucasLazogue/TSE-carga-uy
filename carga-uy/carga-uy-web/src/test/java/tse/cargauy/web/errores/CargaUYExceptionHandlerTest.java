package tse.cargauy.web.errores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import jakarta.ejb.EJBException;
import jakarta.faces.FacesException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerFactory;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ExceptionQueuedEvent;
import jakarta.faces.event.ExceptionQueuedEventContext;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.exceptions.MensajesError;

// en el backoffice un error del negocio se muestra como mensaje en la misma pagina, no como pagina de error
class CargaUYExceptionHandlerTest {

    private final ExceptionHandler envuelto = mock(ExceptionHandler.class);
    private final List<ExceptionQueuedEvent> eventos = new ArrayList<>();
    private FacesContext contexto;

    private abstract static class Contexto extends FacesContext {
        static void usar(FacesContext contexto) {
            setCurrentInstance(contexto);
        }
    }

    @BeforeEach
    void preparar() {
        contexto = mock(FacesContext.class);
        Contexto.usar(contexto);
        when(envuelto.getUnhandledExceptionQueuedEvents()).thenReturn(eventos);
    }

    @AfterEach
    void soltar() {
        Contexto.usar(null);
    }

    private void encolar(Throwable error) {
        ExceptionQueuedEventContext datos = mock(ExceptionQueuedEventContext.class);
        when(datos.getException()).thenReturn(error);
        ExceptionQueuedEvent evento = mock(ExceptionQueuedEvent.class);
        when(evento.getContext()).thenReturn(datos);
        eventos.add(evento);
    }

    // el EJB la envuelve (EJBException, FacesException): se busca en la cadena de causas
    @Test
    void unErrorDelNegocioEnvueltoSeMuestraComoMensaje() {
        CargaUYException negocio = new CargaUYException(CodigoError.NODO_NO_ENCONTRADO, "9");
        encolar(new FacesException(new EJBException(negocio)));

        new CargaUYExceptionHandler(envuelto).handle();

        ArgumentCaptor<FacesMessage> mensaje = ArgumentCaptor.forClass(FacesMessage.class);
        verify(contexto).addMessage(eq(null), mensaje.capture());
        assertEquals(FacesMessage.SEVERITY_ERROR, mensaje.getValue().getSeverity());
        assertEquals(MensajesError.resolver(negocio), mensaje.getValue().getSummary());
        verify(contexto).validationFailed();
        verify(contexto).renderResponse();
        assertEquals(0, eventos.size(), "queda manejado");
        verify(envuelto).handle();
    }

    // cualquier otro error sigue al manejador de JSF (pagina de error)
    @Test
    void otroErrorQuedaParaElManejadorDeJsf() {
        encolar(new IllegalStateException("otra cosa"));

        new CargaUYExceptionHandler(envuelto).handle();

        verify(contexto, never()).addMessage(any(), any());
        assertEquals(1, eventos.size());
        verify(envuelto).handle();
    }

    @Test
    void laFabricaEnvuelveElManejadorDeJsf() {
        ExceptionHandlerFactory fabrica = mock(ExceptionHandlerFactory.class);
        when(fabrica.getExceptionHandler()).thenReturn(envuelto);

        ExceptionHandler manejador = new CargaUYExceptionHandlerFactory(fabrica).getExceptionHandler();

        assertInstanceOf(CargaUYExceptionHandler.class, manejador);
        assertSame(envuelto, ((CargaUYExceptionHandler) manejador).getWrapped());
    }
}
