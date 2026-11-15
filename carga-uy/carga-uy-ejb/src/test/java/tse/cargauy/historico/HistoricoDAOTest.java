package tse.cargauy.historico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import tse.cargauy.dtos.CasoHistoricoDto;
import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.ViajeHistoricoDto;
import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.entities.TipoInfraccion;

class HistoricoDAOTest {

    private static final FiltroInforme MARZO = new FiltroInforme(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null, null);

    private final HistoricoDAO dao = new HistoricoDAO();
    private final Map<String, Object> parametros = new HashMap<>();
    private final List<String> consultas = new ArrayList<>();
    private final List<Object> guardados = new ArrayList<>();
    private final List<String> nativas = new ArrayList<>();
    private final Map<Object, HistCaso> existentes = new HashMap<>();
    private Object bloqueo;

    @BeforeEach
    void preparar() throws Exception {
        TypedQuery<?> query = (TypedQuery<?>) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { TypedQuery.class }, (proxy, metodo, args) -> switch (metodo.getName()) {
                    case "setParameter" -> {
                        parametros.put((String) args[0], args[1]);
                        yield proxy;
                    }
                    case "getResultList" -> List.of();
                    default -> proxy;
                });
        Query nativa = (Query) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { Query.class }, (proxy, metodo, args) -> metodo.getName().equals("executeUpdate") ? 3 : proxy);
        EntityManager entityManager = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { EntityManager.class }, (proxy, metodo, args) -> switch (metodo.getName()) {
                    case "createQuery" -> {
                        consultas.add((String) args[0]);
                        yield query;
                    }
                    case "createNativeQuery" -> {
                        nativas.add((String) args[0]);
                        yield nativa;
                    }
                    case "merge" -> {
                        guardados.add(args[0]);
                        yield args[0];
                    }
                    case "find" -> {
                        bloqueo = args.length > 2 ? args[2] : null;
                        yield existentes.get(args[1]);
                    }
                    default -> null;
                });
        Field campo = HistoricoDAO.class.getDeclaredField("entityManager");
        campo.setAccessible(true);
        campo.set(dao, entityManager);
    }

    private String jpql() {
        return consultas.get(consultas.size() - 1);
    }

    @Test
    void losReportesLeenSoloDelHistorico() {
        dao.contarViajesPorDia(MARZO);
        dao.sumarVolumenPorRubro(MARZO);
        dao.contarInfracciones(MARZO);
        dao.contarViajesPorMesPublico(MARZO, 5);
        dao.sumarVolumenPorRubroPublico(MARZO, 5);

        assertEquals(5, consultas.size());
        consultas.forEach(c -> assertFalse(c.contains("FROM Viaje ") || c.contains("FROM CasoFiscalizacion")));
        assertTrue(consultas.get(0).contains("FROM HistViaje h"));
        assertTrue(consultas.get(1).contains("FROM HistViaje h"));
        assertTrue(consultas.get(2).contains("FROM HistCaso c"));
        assertTrue(consultas.get(3).contains("FROM HistViaje h"));
        assertTrue(consultas.get(4).contains("FROM HistViaje h"));
    }

    @Test
    void elPeriodoIncluyeElUltimoDia() {
        dao.contarViajesPorDia(MARZO);

        assertEquals(Map.of("desde", LocalDate.of(2026, 3, 1).atStartOfDay(), "hasta", LocalDate.of(2026, 4, 1).atStartOfDay()),
                parametros);
    }

    @Test
    void lasInfraccionesFiltranPorTipoDeCarga() {
        dao.contarInfracciones(new FiltroInforme(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), 2L));

        assertTrue(jpql().contains("c.fechaDeteccion < :hasta AND c.idTipoCarga = :idTipoCarga GROUP BY c.tipo"));
        assertEquals(2L, parametros.get("idTipoCarga"));
        assertEquals(EstadoCaso.CONFIRMADA, parametros.get("confirmada"));
        assertEquals(EstadoCaso.DESESTIMADA, parametros.get("desestimada"));
    }

    @Test
    void losViajesPublicosFiltranPorRubroYTipoDeCarga() {
        dao.contarViajesPorMesPublico(new FiltroInforme(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), 2L, 4L), 5);

        assertTrue(jpql().contains("h.idTipoCarga = :idTipoCarga AND h.idRubro = :idRubro GROUP BY"));
        assertEquals(Map.of("umbral", 5L, "desde", LocalDate.of(2026, 3, 1).atStartOfDay(),
                "hasta", LocalDate.of(2026, 4, 1).atStartOfDay(), "idTipoCarga", 2L, "idRubro", 4L), parametros);
    }

    @Test
    void lasFilasBajoElUmbralVuelvenSinValor() {
        dao.contarViajesPorMesPublico(MARZO, 5);
        dao.sumarVolumenPorRubroPublico(MARZO, 5);

        String condicion = "CASE WHEN COUNT(DISTINCT h.idEmpresa) >= :umbral AND COUNT(DISTINCT h.idVehiculo) >= :umbral";
        assertTrue(consultas.get(0).contains(condicion + " THEN COUNT(h) ELSE NULL END"));
        assertTrue(consultas.get(1).contains(condicion + " THEN SUM(h.volumen) ELSE NULL END"));
    }

    @Test
    void elVolumenPublicoSeOrdenaPorRubroYNoPorValor() {
        dao.sumarVolumenPorRubroPublico(MARZO, 5);

        assertTrue(jpql().endsWith("ORDER BY h.rubro"));
        assertFalse(jpql().contains(":idRubro"));
        assertFalse(jpql().contains(":idTipoCarga"));
    }

    @Test
    void guardarReemplazaLaFilaDelViajeODelCaso() {
        dao.guardarViaje(viaje(21L));
        dao.guardarCaso(caso(40L, EstadoCaso.CONFIRMADA));

        assertEquals(21L, ((HistViaje) guardados.get(0)).getIdViaje());
        assertEquals(EstadoCaso.CONFIRMADA, ((HistCaso) guardados.get(1)).getEstado());
    }

    @Test
    void unAvisoAtrasadoNoVuelveElCasoAUnEstadoAnterior() {
        existentes.put(40L, new HistCaso(caso(40L, EstadoCaso.CONFIRMADA)));

        dao.guardarCaso(caso(40L, EstadoCaso.NOTIFICADA));

        assertTrue(guardados.isEmpty());
        assertEquals(LockModeType.PESSIMISTIC_WRITE, bloqueo);
    }

    @Test
    void unAvisoNuevoAvanzaElEstadoDelCaso() {
        existentes.put(40L, new HistCaso(caso(40L, EstadoCaso.NOTIFICADA)));

        dao.guardarCaso(caso(40L, EstadoCaso.EN_DESCARGO));

        assertEquals(EstadoCaso.EN_DESCARGO, ((HistCaso) guardados.get(0)).getEstado());
    }

    @Test
    void reconstruirNoPisaLoQueYaLlegoPorLaCola() {
        assertEquals(3, dao.agregarViajesFaltantes());
        assertTrue(nativas.get(0).startsWith("INSERT INTO historico.hist_viaje"));
        assertTrue(nativas.get(0).contains("WHERE v.estado = 'FINALIZADO'"));
        assertTrue(nativas.get(0).endsWith("ON CONFLICT (idViaje) DO NOTHING"));

        dao.actualizarCasos();
        assertTrue(nativas.get(1).startsWith("INSERT INTO historico.hist_caso"));
        assertTrue(nativas.get(1).contains("ON CONFLICT (idCaso) DO UPDATE SET estado = EXCLUDED.estado WHERE"));
    }

    @Test
    void lasLecturasNoSeSumanALaTransaccionDeQuienLlama() throws Exception {
        for (String metodo : List.of("contarViajesPorDia", "sumarVolumenPorRubro", "contarInfracciones")) {
            assertEquals(TransactionAttributeType.NOT_SUPPORTED, atributo(HistoricoDAO.class.getMethod(metodo, FiltroInforme.class)));
        }
        for (String metodo : List.of("contarViajesPorMesPublico", "sumarVolumenPorRubroPublico")) {
            assertEquals(TransactionAttributeType.NOT_SUPPORTED,
                    atributo(HistoricoDAO.class.getMethod(metodo, FiltroInforme.class, int.class)));
        }
        assertNull(HistoricoDAO.class.getMethod("guardarCaso", CasoHistoricoDto.class).getAnnotation(TransactionAttribute.class));
    }

    private static TransactionAttributeType atributo(Method metodo) {
        return metodo.getAnnotation(TransactionAttribute.class).value();
    }

    private static ViajeHistoricoDto viaje(Long id) {
        return new ViajeHistoricoDto(id, LocalDateTime.of(2026, 3, 10, 18, 0), 1L, 2L, 3L, "Granos", 4L, 30000);
    }

    private static CasoHistoricoDto caso(Long id, EstadoCaso estado) {
        return new CasoHistoricoDto(id, TipoInfraccion.EXCESO_CARGA, estado, LocalDateTime.of(2026, 3, 10, 18, 5), 4L);
    }
}
