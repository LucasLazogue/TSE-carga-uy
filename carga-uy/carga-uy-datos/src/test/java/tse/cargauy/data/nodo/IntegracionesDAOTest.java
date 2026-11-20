package tse.cargauy.data.nodo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;

import tse.cargauy.data.BaseDatos;
import tse.cargauy.data.pesaje.PesajeDAO;
import tse.cargauy.data.refreshtoken.RefreshTokenDAO;
import tse.cargauy.data.vehiculo.VehiculoDAO;
import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.dtos.PesajeDto;
import tse.cargauy.dtos.RefreshTokenDto;
import tse.cargauy.entities.EstadoNodo;
import tse.cargauy.entities.EstadoRefreshToken;
import tse.cargauy.entities.TipoNodo;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

// lo que el central guarda de sus integraciones: los nodos perifericos, los pesajes de las balanzas y las sesiones
// del movil (refresh tokens)
class IntegracionesDAOTest extends BaseDatos {

    @Test
    void cicloDeVidaDeUnNodo() {
        NodoPerifericoDAO nodos = bean(NodoPerifericoDAO.class);
        // el estado inicial lo fija NodoPerifericoEJB: un nodo nuevo queda deshabilitado hasta que se habilita
        NodoPerifericoDto nuevo = new NodoPerifericoDto("balanzas-prueba", TipoNodo.BALANZAS, "https://balanzas-prueba");
        nuevo.setEstado(EstadoNodo.DESHABILITADO);
        nodos.addNodo(nuevo, "clave-cifrada-1");
        sincronizar();
        NodoPerifericoDto nodo = nodos.getByIdentificador("balanzas-prueba");
        Long id = nodo.getId();

        assertEquals(EstadoNodo.DESHABILITADO, nodo.getEstado());
        assertEquals("clave-cifrada-1", nodos.getClaveCifrada(id));
        assertTrue(nodos.getVigentes().stream().anyMatch(n -> n.getId().equals(id)));
        assertFalse(nodos.getHabilitados(TipoNodo.BALANZAS).stream().anyMatch(n -> n.getId().equals(id)));

        nodos.cambiarEstado(id, EstadoNodo.HABILITADO);
        nodos.cambiarClave(id, "clave-cifrada-2");
        nodos.cambiarPuntoAcceso(id, "https://balanzas-prueba-2");
        sincronizar();
        assertTrue(nodos.getHabilitados(TipoNodo.BALANZAS).stream().anyMatch(n -> n.getId().equals(id)));
        assertFalse(nodos.getHabilitados(TipoNodo.TRACKING).stream().anyMatch(n -> n.getId().equals(id)));
        assertEquals("clave-cifrada-2", nodos.getClaveCifrada(id));
        assertEquals("https://balanzas-prueba-2", nodos.getById(id).getPuntoAcceso());

        // la baja es logica: deja de estar vigente pero el nodo sigue en la base
        nodos.cambiarEstado(id, EstadoNodo.BAJA);
        sincronizar();
        assertFalse(nodos.getVigentes().stream().anyMatch(n -> n.getId().equals(id)));
        assertEquals(EstadoNodo.BAJA, nodos.getById(id).getEstado());

        assertNull(nodos.getById(-1L));
        assertNull(nodos.getByIdentificador("no-existe"));
        assertNull(nodos.getClaveCifrada(-1L));
        nodos.cambiarEstado(-1L, EstadoNodo.HABILITADO);
        nodos.cambiarClave(-1L, "x");
        nodos.cambiarPuntoAcceso(-1L, "x");
    }

    private static PesajeDto pesaje(String idOrigen, Long idVehiculo, LocalDateTime fecha) {
        PesajeDto pesaje = new PesajeDto();
        pesaje.setIdOrigen(idOrigen);
        pesaje.setIdNodo(1L);
        pesaje.setIdVehiculo(idVehiculo);
        pesaje.setMatricula("STA1234");
        pesaje.setFechaHora(fecha);
        pesaje.setPesoRegistrado(33000);
        pesaje.setCodigoBalanza("balanza-r5-km30");
        pesaje.setCodigoPeaje("peaje-r5-km30");
        pesaje.setNombrePeaje("Peaje ruta 5 km 30");
        pesaje.setRuta("5");
        pesaje.setKm(30);
        pesaje.setSentido("N");
        pesaje.setRecibidoEn(fecha.plusMinutes(1));
        return pesaje;
    }

    // el mismo pesaje (idOrigen) no se guarda dos veces: la restriccion unica de la base lo rechaza
    @Test
    void pesajesPorVehiculoSinDuplicados() {
        PesajeDAO pesajes = bean(PesajeDAO.class);
        Long sta = bean(VehiculoDAO.class).getVehiculoByMatricula("STA1234").getId();
        LocalDateTime dia = LocalDateTime.of(2031, 2, 1, 10, 0);
        pesajes.registrar(pesaje("balanzas/r5/1", sta, dia));
        pesajes.registrar(pesaje("balanzas/r5/2", sta, dia.plusHours(3)));
        sincronizar();

        assertTrue(pesajes.existe("balanzas/r5/1"));
        assertFalse(pesajes.existe("balanzas/r5/99"));
        List<PesajeDto> delDia = pesajes.getByVehiculo(sta, dia.minusHours(1), dia.plusHours(1));
        assertEquals(1, delDia.size());
        assertEquals(33000, delDia.get(0).getPesoRegistrado());
        assertEquals(2, pesajes.getByVehiculo(sta, dia.minusHours(1), dia.plusDays(1)).size());

        CargaUYException duplicado = assertThrows(CargaUYException.class, () -> pesajes.registrar(pesaje("balanzas/r5/1", sta, dia)));
        assertEquals(CodigoError.PESAJE_YA_REGISTRADO, duplicado.getCodigo());
    }

    private static RefreshTokenDto token(String hash, Long usuario, String dispositivo, Instant emitido) {
        RefreshTokenDto token = new RefreshTokenDto();
        token.setHash(hash);
        token.setIdUsuario(usuario);
        token.setDispositivo(dispositivo);
        token.setEstado(EstadoRefreshToken.ACTIVO);
        token.setEmitido(emitido);
        token.setUltimoUso(emitido);
        token.setExpira(emitido.plus(30, ChronoUnit.DAYS));
        return token;
    }

    @Test
    void sesionesDelMovil() {
        RefreshTokenDAO tokens = bean(RefreshTokenDAO.class);
        Instant ahora = Instant.parse("2031-01-10T12:00:00Z");
        tokens.crear(token("h-1", 5L, "celular-a", ahora));
        tokens.crear(token("h-2", 5L, "celular-b", ahora));
        tokens.crear(token("h-viejo", 5L, "celular-c", ahora.minus(40, ChronoUnit.DAYS)));
        sincronizar();

        tokens.registrarUso("h-1", ahora.plusSeconds(60));
        // al volver a ingresar desde un dispositivo se revocan sus sesiones anteriores, no las de otros
        tokens.revocarActivos(5L, "celular-a");
        sincronizar();
        assertEquals(EstadoRefreshToken.REVOCADO, tokens.getByHash("h-1").getEstado());
        assertEquals(ahora.plusSeconds(60), tokens.getByHash("h-1").getUltimoUso());
        assertEquals(EstadoRefreshToken.ACTIVO, tokens.getByHash("h-2").getEstado());

        tokens.revocar("h-2");
        sincronizar();
        assertEquals(EstadoRefreshToken.REVOCADO, tokens.getByHash("h-2").getEstado());

        assertEquals(1, tokens.borrarViejos(ahora.minus(7, ChronoUnit.DAYS)));
        sincronizar();
        assertNull(tokens.getByHash("h-viejo"));
        assertNull(tokens.getByHash("no-existe"));
    }
}
