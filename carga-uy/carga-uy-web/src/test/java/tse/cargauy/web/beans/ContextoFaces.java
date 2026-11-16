package tse.cargauy.web.beans;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Predicate;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;

// FacesContext simulado para probar los beans fuera del servidor
abstract class ContextoFaces extends FacesContext {

    static FacesContext usar(Predicate<String> tieneRol) {
        FacesContext contexto = mock(FacesContext.class);
        ExternalContext externo = mock(ExternalContext.class);
        when(contexto.getExternalContext()).thenReturn(externo);
        for (String rol : List.of("ADMINISTRADOR", "AUTORIDAD")) {
            when(externo.isUserInRole(rol)).thenReturn(tieneRol.test(rol));
        }
        setCurrentInstance(contexto);
        return contexto;
    }

    static void soltar() {
        setCurrentInstance(null);
    }
}
