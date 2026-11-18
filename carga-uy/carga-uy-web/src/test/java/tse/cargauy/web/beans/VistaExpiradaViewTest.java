package tse.cargauy.web.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class VistaExpiradaViewTest {

    @AfterEach
    void soltar() {
        ContextoFaces.soltar();
    }

    @Test
    void cadaRolVuelveASuPantallaDeInicio() {
        assertEquals("/admin/nodos", PantallaInicio.para(List.of("ADMINISTRADOR")::contains));
        assertEquals("/autoridad/reportes", PantallaInicio.para(List.of("AUTORIDAD")::contains));
        assertEquals("/admin/nodos", PantallaInicio.para(List.of("AUTORIDAD", "ADMINISTRADOR")::contains));
    }

    @Test
    void sinRolDelBackofficeVaAlLogin() {
        assertEquals("/login", PantallaInicio.para(List.of("FUNCIONARIO")::contains));
    }

    @Test
    void conSesionNoMandaAlLogin() {
        ContextoFaces.usar("AUTORIDAD"::equals);
        VistaExpiradaView vista = new VistaExpiradaView();
        assertEquals("/autoridad/reportes", vista.getDestino());
        assertTrue(vista.isConSesion());
    }

    @Test
    void sinSesionMandaAlLogin() {
        ContextoFaces.usar(rol -> false);
        VistaExpiradaView vista = new VistaExpiradaView();
        assertEquals("/login", vista.getDestino());
        assertFalse(vista.isConSesion());
    }
}
