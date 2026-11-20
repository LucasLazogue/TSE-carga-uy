package tse.cargauy.web.ws.salud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import jakarta.json.JsonObject;
import jakarta.ws.rs.core.Response;
import tse.cargauy.data.posicion.AlmacenPosiciones;

// AC22: 200 si puede atender pedidos, 503 si falta alguna de sus bases
class SaludRestTest {

    private final SaludRest rest = new SaludRest();

    private void preparar(boolean baseValida, boolean almacenConectado) throws SQLException {
        Connection conexion = mock(Connection.class);
        when(conexion.isValid(anyInt())).thenReturn(baseValida);
        rest.datos = mock(DataSource.class);
        when(rest.datos.getConnection()).thenReturn(conexion);
        rest.almacen = new AlmacenPosiciones() {
            @Override
            public boolean conectado() {
                return almacenConectado;
            }
        };
    }

    private static JsonObject cuerpo(Response respuesta) {
        return (JsonObject) respuesta.getEntity();
    }

    @Test
    void conTodoArribaResponde200() throws SQLException {
        preparar(true, true);

        Response respuesta = rest.salud();

        assertEquals(200, respuesta.getStatus());
        assertEquals("UP", cuerpo(respuesta).getString("estado"));
        assertEquals("UP", cuerpo(respuesta).getJsonObject("chequeos").getString("base-de-datos"));
        assertEquals("UP", cuerpo(respuesta).getJsonObject("chequeos").getString("almacen-posiciones"));
        verify(rest.datos.getConnection()).close();
    }

    @Test
    void sinElAlmacenDePosicionesResponde503() throws SQLException {
        preparar(true, false);

        Response respuesta = rest.salud();

        assertEquals(503, respuesta.getStatus());
        assertEquals("DOWN", cuerpo(respuesta).getString("estado"));
        assertEquals("DOWN", cuerpo(respuesta).getJsonObject("chequeos").getString("almacen-posiciones"));
    }

    @Test
    void conUnaConexionInvalidaResponde503() throws SQLException {
        preparar(false, true);

        assertEquals(503, rest.salud().getStatus());
    }

    @Test
    void sinBaseDeDatosResponde503() throws SQLException {
        preparar(true, true);
        when(rest.datos.getConnection()).thenThrow(new SQLException("sin conexion"));

        Response respuesta = rest.salud();

        assertEquals(503, respuesta.getStatus());
        assertEquals("DOWN", cuerpo(respuesta).getJsonObject("chequeos").getString("base-de-datos"));
    }
}
