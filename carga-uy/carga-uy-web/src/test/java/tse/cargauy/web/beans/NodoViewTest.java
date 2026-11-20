package tse.cargauy.web.beans;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.entities.EstadoNodo;
import tse.cargauy.entities.TipoNodo;
import tse.cargauy.negocio.nodo.NodoPerifericoEJBLocal;

// la administracion de nodos del backoffice
class NodoViewTest {

    private final NodoPerifericoEJBLocal nodos = mock(NodoPerifericoEJBLocal.class);
    private final NodoView vista = new NodoView();
    private final NodoPerifericoDto existente = new NodoPerifericoDto("nodo-1", TipoNodo.values()[0], "https://nodo-1");
    private FacesContext contexto;

    @BeforeEach
    void preparar() throws ReflectiveOperationException {
        contexto = ContextoFaces.usar(rol -> true);
        Field ejb = NodoView.class.getDeclaredField("nodoEJB");
        ejb.setAccessible(true);
        ejb.set(vista, nodos);
        existente.setId(3L);
        when(nodos.getVigentes()).thenReturn(List.of(existente));
        vista.init();
    }

    @AfterEach
    void soltar() {
        ContextoFaces.soltar();
    }

    private String ultimoMensaje() {
        ArgumentCaptor<FacesMessage> mensaje = ArgumentCaptor.forClass(FacesMessage.class);
        verify(contexto, org.mockito.Mockito.atLeastOnce()).addMessage(eq(null), mensaje.capture());
        return mensaje.getValue().getSummary();
    }

    @Test
    void cargaLosNodosAlAbrir() {
        assertEquals(List.of(existente), vista.getNodos());
        assertArrayEquals(TipoNodo.values(), vista.getTipos());
    }

    // despues del alta el formulario queda vacio: la clave no queda en el estado de la pagina
    @Test
    void altaYFormularioNuevo() {
        NodoPerifericoDto nuevo = vista.getNodo();
        nuevo.setIdentificador("nodo-2");
        nuevo.setClave("secreta");

        vista.createNodo();

        verify(nodos).addNodo(nuevo);
        assertTrue(ultimoMensaje().contains("nodo-2"));
        assertNotSame(nuevo, vista.getNodo());
        assertNull(vista.getNodo().getClave());
        verify(nodos, times(2)).getVigentes();
    }

    @Test
    void accionesSobreUnNodo() {
        existente.setClave("otra");

        vista.habilitar(existente);
        vista.deshabilitar(existente);
        vista.cambiarPuntoAcceso(existente);
        vista.cambiarClave(existente);
        vista.darDeBaja(existente);

        verify(nodos).habilitar(3L);
        verify(nodos).deshabilitar(3L);
        verify(nodos).cambiarPuntoAcceso(3L, "https://nodo-1");
        verify(nodos).cambiarClave(3L, "otra");
        verify(nodos).darDeBaja(3L);
        verify(contexto, times(5)).addMessage(eq(null), any(FacesMessage.class));
        assertTrue(ultimoMensaje().contains("dado de baja"));
    }

    @Test
    void habilitadoSegunElEstado() {
        existente.setEstado(EstadoNodo.HABILITADO);
        assertTrue(vista.habilitado(existente));

        for (EstadoNodo estado : EstadoNodo.values()) {
            if (estado != EstadoNodo.HABILITADO) {
                existente.setEstado(estado);
                assertFalse(vista.habilitado(existente));
            }
        }
    }

    @Test
    void nuevoYSetNodo() {
        vista.setNodo(existente);
        assertSame(existente, vista.getNodo());

        vista.nuevo();
        assertNotSame(existente, vista.getNodo());
    }
}
