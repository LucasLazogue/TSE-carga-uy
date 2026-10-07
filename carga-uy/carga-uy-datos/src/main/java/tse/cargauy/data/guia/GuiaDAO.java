package tse.cargauy.data.guia;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import tse.cargauy.data.usuario.UsuarioDAOLocal;
import tse.cargauy.dtos.FiltroGuias;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;
import tse.cargauy.entities.Empresa;
import tse.cargauy.entities.Empresa_;
import tse.cargauy.entities.Guia;
import tse.cargauy.entities.Guia_;
import tse.cargauy.entities.Responsable;
import tse.cargauy.entities.Rubro;
import tse.cargauy.entities.TipoCarga;
import tse.cargauy.entities.Viaje;
import tse.cargauy.entities.Viaje_;

@Stateless
public class GuiaDAO implements GuiaDAOLocal {

    @PersistenceContext(unitName = "carga-uy")
    private EntityManager entityManager;

    @EJB
    private UsuarioDAOLocal usuarioDAO;

    @Override
    public GuiaDto getGuiaById(Long id) {
        Guia guia = entityManager.find(Guia.class, id);
        return guia == null ? null : Serializers.toDto(guia);
    }

    @Override
    public List<GuiaDto> getAll() {
        return toDtos(entityManager.createQuery("SELECT g FROM Guia g ORDER BY g.fecha DESC", Guia.class).getResultList());
    }

    @Override
    public PaginaDto<GuiaDto> buscar(FiltroGuias filtro, Paginacion paginacion) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Guia> query = cb.createQuery(Guia.class);
        Root<Guia> guia = query.from(Guia.class);
        query.select(guia)
                .where(condiciones(filtro, cb, query, guia))
                .orderBy(cb.desc(guia.get(Guia_.fecha)), cb.desc(guia.get(Guia_.id)));
        List<GuiaDto> items = toDtos(entityManager.createQuery(query)
                .setFirstResult(paginacion.getDesde())
                .setMaxResults(paginacion.getTamanio())
                .getResultList());

        CriteriaQuery<Long> conteo = cb.createQuery(Long.class);
        Root<Guia> contada = conteo.from(Guia.class);
        conteo.select(cb.count(contada)).where(condiciones(filtro, cb, conteo, contada));
        long total = entityManager.createQuery(conteo).getSingleResult();

        return new PaginaDto<>(items, total, paginacion.getPagina(), paginacion.getTamanio());
    }

    private Predicate[] condiciones(FiltroGuias filtro, CriteriaBuilder cb, AbstractQuery<?> query, Root<Guia> guia) {
        List<Predicate> condiciones = new ArrayList<>();
        if (filtro.getIdsEmpresa() != null) {
            condiciones.add(filtro.getIdsEmpresa().isEmpty()
                    ? cb.disjunction()
                    : guia.get(Guia_.empresa).get(Empresa_.id).in(filtro.getIdsEmpresa()));
        }
        if (filtro.getBusqueda() != null && !filtro.getBusqueda().isBlank()) {
            String numero = filtro.getBusqueda().replaceAll("\\D", "");
            condiciones.add(numero.isEmpty() || numero.length() > 18
                    ? cb.disjunction()
                    : cb.equal(guia.get(Guia_.id), Long.valueOf(numero)));
        }
        if (filtro.getConViaje() != null) {
            Subquery<Long> viaje = query.subquery(Long.class);
            Root<Viaje> v = viaje.from(Viaje.class);
            viaje.select(v.get(Viaje_.id)).where(cb.equal(v.get(Viaje_.guia), guia));
            condiciones.add(Boolean.TRUE.equals(filtro.getConViaje()) ? cb.exists(viaje) : cb.not(cb.exists(viaje)));
        }
        if (filtro.getDesde() != null) {
            condiciones.add(cb.greaterThanOrEqualTo(guia.get(Guia_.fecha), filtro.getDesde()));
        }
        if (filtro.getHasta() != null) {
            condiciones.add(cb.lessThanOrEqualTo(guia.get(Guia_.fecha), filtro.getHasta()));
        }
        return condiciones.toArray(Predicate[]::new);
    }

    @Override
    public GuiaDto addGuia(GuiaDto guiaDto) {
        Long responsable = usuarioDAO.getResponsableVigente(guiaDto.getIdRegistradaPor(), guiaDto.getIdEmpresa(), guiaDto.getFecha());
        Guia guia = Serializers.toEntity(guiaDto,
                entityManager.find(Empresa.class, guiaDto.getIdEmpresa()),
                entityManager.find(Responsable.class, responsable),
                entityManager.find(Rubro.class, guiaDto.getIdRubro()),
                entityManager.find(TipoCarga.class, guiaDto.getIdTipoCarga()));
        entityManager.persist(guia);
        return Serializers.toDto(guia);
    }

    @Override
    public void updateGuia(Long id, GuiaDto guiaDto) {
        Guia guia = entityManager.find(Guia.class, id);
        if (guia != null) {
            guia.setFecha(guiaDto.getFecha());
            guia.setOrigenLat(guiaDto.getOrigenLat());
            guia.setOrigenLon(guiaDto.getOrigenLon());
            guia.setDestinoLat(guiaDto.getDestinoLat());
            guia.setDestinoLon(guiaDto.getDestinoLon());
            guia.setVolumen(guiaDto.getVolumen());
            guia.setRubro(entityManager.find(Rubro.class, guiaDto.getIdRubro()));
            guia.setTipoCarga(entityManager.find(TipoCarga.class, guiaDto.getIdTipoCarga()));
        }
    }

    @Override
    public void deleteGuia(Long id) {
        Guia guia = entityManager.find(Guia.class, id);
        if (guia != null) {
            entityManager.remove(guia);
        }
    }

    @Override
    public List<RubroDto> getRubros() {
        List<RubroDto> res = new ArrayList<>();
        for (Rubro r : entityManager.createQuery("SELECT r FROM Rubro r ORDER BY r.nombre", Rubro.class).getResultList()) {
            res.add(Serializers.toDto(r));
        }
        return res;
    }

    @Override
    public RubroDto getRubroById(Long id) {
        Rubro rubro = entityManager.find(Rubro.class, id);
        return rubro == null ? null : Serializers.toDto(rubro);
    }

    @Override
    public void addRubro(String nombre) {
        entityManager.persist(new Rubro(nombre));
    }

    @Override
    public List<TipoCargaDto> getTiposCarga() {
        List<TipoCargaDto> res = new ArrayList<>();
        for (TipoCarga t : entityManager.createQuery("SELECT t FROM TipoCarga t ORDER BY t.nombre", TipoCarga.class).getResultList()) {
            res.add(Serializers.toDto(t));
        }
        return res;
    }

    @Override
    public TipoCargaDto getTipoCargaById(Long id) {
        TipoCarga tipoCarga = entityManager.find(TipoCarga.class, id);
        return tipoCarga == null ? null : Serializers.toDto(tipoCarga);
    }

    @Override
    public void addTipoCarga(String nombre) {
        entityManager.persist(new TipoCarga(nombre));
    }

    private List<GuiaDto> toDtos(List<Guia> guias) {
        List<GuiaDto> res = new ArrayList<>();
        for (Guia g : guias) {
            res.add(Serializers.toDto(g));
        }
        return res;
    }
}
