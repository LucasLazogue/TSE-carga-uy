package tse.cargauy.data.viaje;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.data.CriteriaGrabada;
import tse.cargauy.dtos.FiltroViajes;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.ViajeDto;

class ViajeDAOTest {

    private final ViajeDAO dao = new ViajeDAO();
    private final CriteriaGrabada criteria = new CriteriaGrabada();

    @BeforeEach
    void preparar() throws Exception {
        Field campo = ViajeDAO.class.getDeclaredField("entityManager");
        campo.setAccessible(true);
        campo.set(dao, criteria.entityManager());
    }

    @Test
    void elTotalDeUnaEmpresaCuentaLaGuiaDeCadaViajeYNoLaFila() {
        FiltroViajes filtro = new FiltroViajes();
        filtro.setIdsEmpresa(List.of(7L));
        criteria.total = 1600;

        PaginaDto<ViajeDto> pagina = dao.getAll(filtro, new Paginacion(0, 20));

        assertEquals(1600, pagina.getTotal());
        // la segunda raiz es la del conteo: se cuenta un camino que sale de ella (la guia), no la raiz misma
        Object raizConteo = criteria.de("from").get(1).resultado();
        Object contado = criteria.de("count").get(0).argumentos().get(0);
        assertNotSame(raizConteo, contado);
        assertSame(raizConteo, criteria.llamadas.stream()
                .filter(l -> l.metodo().equals("get") && l.resultado() == contado)
                .findFirst().orElseThrow().objeto());
    }
}
