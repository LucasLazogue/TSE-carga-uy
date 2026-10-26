package tse.cargauy.data.informe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.InfraccionesPorTipoDto;
import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.entities.TipoInfraccion;

class InformeDAOTest {

    private final InformeDAO dao = new InformeDAO();
    private final List<Object[]> filas = new ArrayList<>();

    @BeforeEach
    void preparar() throws Exception {
        TypedQuery<?> query = (TypedQuery<?>) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { TypedQuery.class }, (proxy, metodo, args) ->
                        metodo.getName().equals("getResultList") ? filas : proxy);
        EntityManager entityManager = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { EntityManager.class }, (proxy, metodo, args) -> query);
        Field campo = InformeDAO.class.getDeclaredField("entityManager");
        campo.setAccessible(true);
        campo.set(dao, entityManager);
    }

    @Test
    void lasInfraccionesSeAgrupanPorTipoYSeparanConfirmadasDePendientes() {
        filas.add(new Object[] { TipoInfraccion.EXCESO_CARGA, EstadoCaso.CONFIRMADA, 3L });
        filas.add(new Object[] { TipoInfraccion.EXCESO_CARGA, EstadoCaso.DESESTIMADA, 1L });
        filas.add(new Object[] { TipoInfraccion.EXCESO_CARGA, EstadoCaso.NOTIFICADA, 2L });
        filas.add(new Object[] { TipoInfraccion.DESVIO_RUTA, EstadoCaso.EN_DESCARGO, 1L });

        List<InfraccionesPorTipoDto> infracciones = dao.contarInfracciones(
                new FiltroInforme(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 26), null));

        assertEquals(TipoInfraccion.values().length, infracciones.size());
        InfraccionesPorTipoDto exceso = infracciones.get(TipoInfraccion.EXCESO_CARGA.ordinal());
        assertEquals(6, exceso.getDetectadas());
        assertEquals(3, exceso.getConfirmadas());
        assertEquals(2, exceso.getPendientes());
        InfraccionesPorTipoDto desvio = infracciones.get(TipoInfraccion.DESVIO_RUTA.ordinal());
        assertEquals(1, desvio.getDetectadas());
        assertEquals(0, desvio.getConfirmadas());
        assertEquals(1, desvio.getPendientes());
        assertEquals(0, infracciones.get(TipoInfraccion.SIN_HABILITACION.ordinal()).getDetectadas());
    }
}
