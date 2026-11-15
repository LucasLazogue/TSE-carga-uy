package tse.cargauy.historico;

import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import tse.cargauy.dtos.CasoHistoricoDto;
import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.InfraccionesPorTipoDto;
import tse.cargauy.dtos.ViajeHistoricoDto;
import tse.cargauy.dtos.ViajesPorMesPublicoDto;
import tse.cargauy.dtos.ViajesPorPeriodoDto;
import tse.cargauy.dtos.VolumenPorRubroDto;
import tse.cargauy.dtos.VolumenPorRubroPublicoDto;
import tse.cargauy.entities.EstadoCaso;

@Stateless
public class HistoricoDAO implements HistoricoDAOLocal {

    private static final int TIEMPO_MAXIMO_MS = 10000;

    private static final String DIA = "EXTRACT(DATE FROM h.fechaFin)";
    private static final String MES = "EXTRACT(YEAR FROM h.fechaFin), EXTRACT(MONTH FROM h.fechaFin)";

    static final String ORDEN_ESTADOS = "ARRAY['DETECTADA','NOTIFICADA','EN_DESCARGO','CONFIRMADA','DESESTIMADA']";

    static final String VIAJES_FALTANTES = "INSERT INTO historico.hist_viaje"
            + " (idViaje, fechaFin, idEmpresa, idVehiculo, idRubro, rubro, idTipoCarga, volumen)"
            + " SELECT v.id, v.fechaFin, g.empresa_id, v.vehiculo_id, g.rubro_id, r.nombre, g.tipoCarga_id, g.volumen"
            + " FROM viaje v JOIN guia g ON g.id = v.guia_id JOIN rubro r ON r.id = g.rubro_id"
            + " WHERE v.estado = 'FINALIZADO'"
            + " ON CONFLICT (idViaje) DO NOTHING";

    static final String CASOS_FALTANTES = "INSERT INTO historico.hist_caso (idCaso, tipo, estado, fechaDeteccion, idTipoCarga)"
            + " SELECT c.id, c.tipo, c.estado, c.fechaDeteccion, g.tipoCarga_id"
            + " FROM casofiscalizacion c LEFT JOIN viaje v ON v.id = c.viaje_id LEFT JOIN guia g ON g.id = v.guia_id"
            + " ON CONFLICT (idCaso) DO UPDATE SET estado = EXCLUDED.estado"
            + " WHERE array_position(" + ORDEN_ESTADOS + ", EXCLUDED.estado::text)"
            + " > array_position(" + ORDEN_ESTADOS + ", historico.hist_caso.estado::text)";

    @PersistenceContext(unitName = "historico")
    private EntityManager entityManager;

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<ViajesPorPeriodoDto> contarViajesPorDia(FiltroInforme filtro) {
        return viajes("SELECT NEW tse.cargauy.dtos.ViajesPorPeriodoDto(" + DIA + ", COUNT(h))",
                " GROUP BY " + DIA + " ORDER BY " + DIA, filtro, ViajesPorPeriodoDto.class)
                .getResultList();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<VolumenPorRubroDto> sumarVolumenPorRubro(FiltroInforme filtro) {
        return viajes("SELECT NEW tse.cargauy.dtos.VolumenPorRubroDto(h.rubro, SUM(h.volumen))",
                " GROUP BY h.rubro ORDER BY SUM(h.volumen) DESC, h.rubro", filtro, VolumenPorRubroDto.class)
                .getResultList();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<InfraccionesPorTipoDto> contarInfracciones(FiltroInforme filtro) {
        String tipoCarga = filtro.getIdTipoCarga() == null ? "" : " AND c.idTipoCarga = :idTipoCarga";
        TypedQuery<InfraccionesPorTipoDto> query = consulta(
                        "SELECT NEW tse.cargauy.dtos.InfraccionesPorTipoDto(c.tipo, COUNT(c),"
                                + " SUM(CASE WHEN c.estado = :confirmada THEN 1 ELSE 0 END),"
                                + " SUM(CASE WHEN c.estado = :desestimada THEN 1 ELSE 0 END))"
                                + " FROM HistCaso c"
                                + " WHERE c.fechaDeteccion >= :desde AND c.fechaDeteccion < :hasta" + tipoCarga
                                + " GROUP BY c.tipo", InfraccionesPorTipoDto.class)
                .setParameter("confirmada", EstadoCaso.CONFIRMADA)
                .setParameter("desestimada", EstadoCaso.DESESTIMADA)
                .setParameter("desde", filtro.getDesde().atStartOfDay())
                .setParameter("hasta", filtro.getHasta().plusDays(1).atStartOfDay());
        if (filtro.getIdTipoCarga() != null) {
            query.setParameter("idTipoCarga", filtro.getIdTipoCarga());
        }
        return query.getResultList();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<ViajesPorMesPublicoDto> contarViajesPorMesPublico(FiltroInforme filtro, int umbral) {
        return viajes("SELECT NEW tse.cargauy.dtos.ViajesPorMesPublicoDto(" + MES + ", " + siSuperaUmbral("COUNT(h)") + ")",
                " GROUP BY " + MES + " ORDER BY " + MES, filtro, ViajesPorMesPublicoDto.class)
                .setParameter("umbral", (long) umbral)
                .getResultList();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<VolumenPorRubroPublicoDto> sumarVolumenPorRubroPublico(FiltroInforme filtro, int umbral) {
        return viajes("SELECT NEW tse.cargauy.dtos.VolumenPorRubroPublicoDto(h.rubro, " + siSuperaUmbral("SUM(h.volumen)") + ")",
                " GROUP BY h.rubro ORDER BY h.rubro", filtro, VolumenPorRubroPublicoDto.class)
                .setParameter("umbral", (long) umbral)
                .getResultList();
    }

    @Override
    public void guardarViaje(ViajeHistoricoDto viaje) {
        entityManager.merge(new HistViaje(viaje));
    }

    // los avisos pueden llegar desordenados (reintentos, varias instancias): un estado anterior no pisa uno posterior
    @Override
    public void guardarCaso(CasoHistoricoDto caso) {
        HistCaso actual = entityManager.find(HistCaso.class, caso.getIdCaso(), LockModeType.PESSIMISTIC_WRITE);
        if (actual != null && caso.getEstado().ordinal() < actual.getEstado().ordinal()) {
            return;
        }
        entityManager.merge(new HistCaso(caso));
    }

    @Override
    public int agregarViajesFaltantes() {
        return entityManager.createNativeQuery(VIAJES_FALTANTES).executeUpdate();
    }

    @Override
    public int actualizarCasos() {
        return entityManager.createNativeQuery(CASOS_FALTANTES).executeUpdate();
    }

    private static String siSuperaUmbral(String valor) {
        return "CASE WHEN COUNT(DISTINCT h.idEmpresa) >= :umbral AND COUNT(DISTINCT h.idVehiculo) >= :umbral"
                + " THEN " + valor + " ELSE NULL END";
    }

    private <T> TypedQuery<T> viajes(String seleccion, String agrupamiento, FiltroInforme filtro, Class<T> tipo) {
        String tipoCarga = filtro.getIdTipoCarga() == null ? "" : " AND h.idTipoCarga = :idTipoCarga";
        String rubro = filtro.getIdRubro() == null ? "" : " AND h.idRubro = :idRubro";
        TypedQuery<T> query = consulta(seleccion + " FROM HistViaje h WHERE h.fechaFin >= :desde AND h.fechaFin < :hasta"
                        + tipoCarga + rubro + agrupamiento, tipo)
                .setParameter("desde", filtro.getDesde().atStartOfDay())
                .setParameter("hasta", filtro.getHasta().plusDays(1).atStartOfDay());
        if (filtro.getIdTipoCarga() != null) {
            query.setParameter("idTipoCarga", filtro.getIdTipoCarga());
        }
        if (filtro.getIdRubro() != null) {
            query.setParameter("idRubro", filtro.getIdRubro());
        }
        return query;
    }

    private <T> TypedQuery<T> consulta(String jpql, Class<T> tipo) {
        return entityManager.createQuery(jpql, tipo).setHint("jakarta.persistence.query.timeout", TIEMPO_MAXIMO_MS);
    }
}
