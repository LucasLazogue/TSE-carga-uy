package tse.cargauy.data.informe;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import tse.cargauy.dtos.ConteoDto;
import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.InfraccionesPorTipoDto;
import tse.cargauy.dtos.ViajesPorDiaDto;
import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.entities.EstadoPermiso;
import tse.cargauy.entities.EstadoViaje;
import tse.cargauy.entities.TipoInfraccion;

@Stateless
public class InformeDAO implements InformeDAOLocal {

    private static final int TIEMPO_MAXIMO_MS = 10000;

    @PersistenceContext(unitName = "carga-uy")
    private EntityManager entityManager;

    @Override
    public long contarVehiculosHabilitados(LocalDate fecha) {
        return habilitados("p.vehiculo.id", fecha);
    }

    @Override
    public long contarEmpresasHabilitadas(LocalDate fecha) {
        return habilitados("p.vehiculo.empresa.id", fecha);
    }

    private long habilitados(String campo, LocalDate fecha) {
        return consulta("SELECT COUNT(DISTINCT " + campo + ") FROM Permiso p WHERE p.estado = :aprobado"
                        + " AND p.validoDesde <= :fecha AND p.validoHasta >= :fecha", Long.class)
                .setParameter("aprobado", EstadoPermiso.APROBADO)
                .setParameter("fecha", fecha)
                .getSingleResult();
    }

    @Override
    public List<ViajesPorDiaDto> contarViajesPorDia(FiltroInforme filtro) {
        return viajesRealizados("SELECT EXTRACT(DATE FROM v.fechaFin), COUNT(v)", filtro,
                " GROUP BY EXTRACT(DATE FROM v.fechaFin) ORDER BY EXTRACT(DATE FROM v.fechaFin)")
                .getResultStream()
                .map(f -> new ViajesPorDiaDto((LocalDate) f[0], (Long) f[1]))
                .toList();
    }

    @Override
    public List<ConteoDto> sumarVolumenPorRubro(FiltroInforme filtro) {
        return viajesRealizados("SELECT v.guia.rubro.nombre, SUM(v.guia.volumen)", filtro,
                " GROUP BY v.guia.rubro.nombre ORDER BY SUM(v.guia.volumen) DESC, v.guia.rubro.nombre")
                .getResultStream()
                .map(f -> new ConteoDto((String) f[0], (Long) f[1]))
                .toList();
    }

    private TypedQuery<Object[]> viajesRealizados(String select, FiltroInforme filtro, String agrupar) {
        String tipoCarga = filtro.getIdTipoCarga() == null ? "" : " AND v.guia.tipoCarga.id = :idTipoCarga";
        TypedQuery<Object[]> query = consulta(select + " FROM Viaje v WHERE v.estado = :finalizado"
                        + " AND v.fechaFin >= :desde AND v.fechaFin < :hasta" + tipoCarga + agrupar, Object[].class)
                .setParameter("finalizado", EstadoViaje.FINALIZADO);
        return periodo(query, filtro);
    }

    @Override
    public List<InfraccionesPorTipoDto> contarInfracciones(FiltroInforme filtro) {
        String tipoCarga = filtro.getIdTipoCarga() == null ? "" : " AND c.viaje.guia.tipoCarga.id = :idTipoCarga";
        List<Object[]> filas = periodo(consulta("SELECT c.tipo, c.estado, COUNT(c) FROM CasoFiscalizacion c"
                        + " WHERE c.fechaDeteccion >= :desde AND c.fechaDeteccion < :hasta" + tipoCarga
                        + " GROUP BY c.tipo, c.estado", Object[].class), filtro)
                .getResultList();

        Map<TipoInfraccion, InfraccionesPorTipoDto> porTipo = new EnumMap<>(TipoInfraccion.class);
        for (TipoInfraccion tipo : TipoInfraccion.values()) {
            porTipo.put(tipo, new InfraccionesPorTipoDto(tipo));
        }
        for (Object[] f : filas) {
            InfraccionesPorTipoDto infracciones = porTipo.get((TipoInfraccion) f[0]);
            EstadoCaso estado = (EstadoCaso) f[1];
            long cantidad = (Long) f[2];
            infracciones.setDetectadas(infracciones.getDetectadas() + cantidad);
            if (estado == EstadoCaso.CONFIRMADA) {
                infracciones.setConfirmadas(infracciones.getConfirmadas() + cantidad);
            } else if (estado != EstadoCaso.DESESTIMADA) {
                infracciones.setPendientes(infracciones.getPendientes() + cantidad);
            }
        }
        return new ArrayList<>(porTipo.values());
    }

    private <T> TypedQuery<T> periodo(TypedQuery<T> query, FiltroInforme filtro) {
        query.setParameter("desde", filtro.getDesde().atStartOfDay())
                .setParameter("hasta", filtro.getHasta().plusDays(1).atStartOfDay());
        if (filtro.getIdTipoCarga() != null) {
            query.setParameter("idTipoCarga", filtro.getIdTipoCarga());
        }
        return query;
    }

    private <T> TypedQuery<T> consulta(String jpql, Class<T> tipo) {
        return entityManager.createQuery(jpql, tipo).setHint("jakarta.persistence.query.timeout", TIEMPO_MAXIMO_MS);
    }
}
