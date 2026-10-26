package tse.cargauy.data.informe;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.InformeDto;
import tse.cargauy.dtos.InfraccionesPorTipoDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.ViajesPorPeriodoDto;
import tse.cargauy.dtos.VolumenPorRubroDto;
import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.entities.EstadoInforme;
import tse.cargauy.entities.EstadoPermiso;
import tse.cargauy.entities.EstadoViaje;
import tse.cargauy.entities.Informe;
import tse.cargauy.entities.TipoCarga;
import tse.cargauy.entities.UsuarioBackoffice;

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
                        + " AND p.validoDesde <= :fecha AND p.validoHasta >= :fecha"
                        + " AND EXISTS (SELECT i FROM Itv i WHERE i.vehiculo = p.vehiculo"
                        + " AND i.fechaInspeccion <= :fecha AND i.fechaVencimiento >= :fecha)", Long.class)
                .setParameter("aprobado", EstadoPermiso.APROBADO)
                .setParameter("fecha", fecha)
                .getSingleResult();
    }

    @Override
    public List<ViajesPorPeriodoDto> contarViajesPorDia(FiltroInforme filtro) {
        String tipoCarga = filtro.getIdTipoCarga() == null ? "" : " AND v.guia.tipoCarga.id = :idTipoCarga";
        TypedQuery<ViajesPorPeriodoDto> query = consulta(
                        "SELECT NEW tse.cargauy.dtos.ViajesPorPeriodoDto(EXTRACT(DATE FROM v.fechaFin), COUNT(v))"
                                + " FROM Viaje v WHERE v.estado = :finalizado"
                                + " AND v.fechaFin >= :desde AND v.fechaFin < :hasta" + tipoCarga
                                + " GROUP BY EXTRACT(DATE FROM v.fechaFin) ORDER BY EXTRACT(DATE FROM v.fechaFin)",
                        ViajesPorPeriodoDto.class)
                .setParameter("finalizado", EstadoViaje.FINALIZADO)
                .setParameter("desde", filtro.getDesde().atStartOfDay())
                .setParameter("hasta", filtro.getHasta().plusDays(1).atStartOfDay());
        if (filtro.getIdTipoCarga() != null) {
            query.setParameter("idTipoCarga", filtro.getIdTipoCarga());
        }
        return query.getResultList();
    }

    @Override
    public List<VolumenPorRubroDto> sumarVolumenPorRubro(FiltroInforme filtro) {
        String tipoCarga = filtro.getIdTipoCarga() == null ? "" : " AND v.guia.tipoCarga.id = :idTipoCarga";
        TypedQuery<VolumenPorRubroDto> query = consulta(
                        "SELECT NEW tse.cargauy.dtos.VolumenPorRubroDto(v.guia.rubro.nombre, SUM(v.guia.volumen))"
                                + " FROM Viaje v WHERE v.estado = :finalizado"
                                + " AND v.fechaFin >= :desde AND v.fechaFin < :hasta" + tipoCarga
                                + " GROUP BY v.guia.rubro.nombre ORDER BY SUM(v.guia.volumen) DESC, v.guia.rubro.nombre",
                        VolumenPorRubroDto.class)
                .setParameter("finalizado", EstadoViaje.FINALIZADO)
                .setParameter("desde", filtro.getDesde().atStartOfDay())
                .setParameter("hasta", filtro.getHasta().plusDays(1).atStartOfDay());
        if (filtro.getIdTipoCarga() != null) {
            query.setParameter("idTipoCarga", filtro.getIdTipoCarga());
        }
        return query.getResultList();
    }

    @Override
    public List<InfraccionesPorTipoDto> contarInfracciones(FiltroInforme filtro) {
        String tipoCarga = filtro.getIdTipoCarga() == null ? "" : " AND c.viaje.guia.tipoCarga.id = :idTipoCarga";
        TypedQuery<InfraccionesPorTipoDto> query = consulta(
                        "SELECT NEW tse.cargauy.dtos.InfraccionesPorTipoDto(c.tipo, COUNT(c),"
                                + " SUM(CASE WHEN c.estado = :confirmada THEN 1 ELSE 0 END),"
                                + " SUM(CASE WHEN c.estado = :desestimada THEN 1 ELSE 0 END))"
                                + " FROM CasoFiscalizacion c"
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
    public InformeDto addInforme(InformeDto informeDto) {
        Informe informe = Serializers.toEntity(informeDto,
                informeDto.getIdTipoCarga() == null ? null : entityManager.find(TipoCarga.class, informeDto.getIdTipoCarga()),
                entityManager.find(UsuarioBackoffice.class, informeDto.getIdSolicitante()));
        entityManager.persist(informe);
        return Serializers.toDto(informe);
    }

    @Override
    public PaginaDto<InformeDto> getBySolicitante(Long idSolicitante, Paginacion paginacion) {
        List<InformeDto> items = entityManager.createQuery(
                        "SELECT i FROM Informe i WHERE i.solicitante.id = :idSolicitante ORDER BY i.fechaSolicitud DESC, i.id DESC",
                        Informe.class)
                .setParameter("idSolicitante", idSolicitante)
                .setFirstResult(paginacion.getDesde())
                .setMaxResults(paginacion.getTamanio())
                .getResultStream()
                .map(Serializers::toDto)
                .toList();
        long total = entityManager.createQuery("SELECT COUNT(i) FROM Informe i WHERE i.solicitante.id = :idSolicitante", Long.class)
                .setParameter("idSolicitante", idSolicitante)
                .getSingleResult();
        return new PaginaDto<>(items, total, paginacion.getPagina(), paginacion.getTamanio());
    }

    @Override
    public InformeDto getInformeById(Long id) {
        Informe informe = entityManager.find(Informe.class, id);
        return informe == null ? null : Serializers.toDto(informe);
    }

    @Override
    public boolean tienePendientes(Long idSolicitante) {
        return entityManager.createQuery(
                        "SELECT COUNT(i) FROM Informe i WHERE i.solicitante.id = :idSolicitante AND i.estado = :pendiente", Long.class)
                .setParameter("idSolicitante", idSolicitante)
                .setParameter("pendiente", EstadoInforme.PENDIENTE)
                .getSingleResult() > 0;
    }

    @Override
    public void guardarContenido(Long id, byte[] contenido, LocalDateTime fecha) {
        entityManager.createQuery("UPDATE Informe i SET i.estado = :generado, i.contenido = :contenido, i.fechaGeneracion = :fecha"
                        + " WHERE i.id = :id AND i.estado = :pendiente")
                .setParameter("generado", EstadoInforme.GENERADO)
                .setParameter("contenido", contenido)
                .setParameter("fecha", fecha)
                .setParameter("id", id)
                .setParameter("pendiente", EstadoInforme.PENDIENTE)
                .executeUpdate();
    }

    @Override
    public void marcarFallido(Long id) {
        entityManager.createQuery("UPDATE Informe i SET i.estado = :fallido WHERE i.id = :id AND i.estado = :pendiente")
                .setParameter("fallido", EstadoInforme.FALLIDO)
                .setParameter("id", id)
                .setParameter("pendiente", EstadoInforme.PENDIENTE)
                .executeUpdate();
    }

    @Override
    public int vencer(LocalDateTime generadosAntesDe) {
        return entityManager.createQuery("UPDATE Informe i SET i.estado = :vencido, i.contenido = NULL"
                        + " WHERE i.estado = :generado AND i.fechaGeneracion < :limite")
                .setParameter("vencido", EstadoInforme.VENCIDO)
                .setParameter("generado", EstadoInforme.GENERADO)
                .setParameter("limite", generadosAntesDe)
                .executeUpdate();
    }

    @Override
    public byte[] getContenido(Long id) {
        Informe informe = entityManager.find(Informe.class, id);
        return informe == null ? null : informe.getContenido();
    }

    private <T> TypedQuery<T> consulta(String jpql, Class<T> tipo) {
        return entityManager.createQuery(jpql, tipo).setHint("jakarta.persistence.query.timeout", TIEMPO_MAXIMO_MS);
    }
}
