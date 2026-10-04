package tse.cargauy.data.guia;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;
import tse.cargauy.entities.Ciudadano;
import tse.cargauy.entities.Empresa;
import tse.cargauy.entities.Guia;
import tse.cargauy.entities.Rubro;
import tse.cargauy.entities.TipoCarga;

@Stateless
public class GuiaDAO implements GuiaDAOLocal {

    @PersistenceContext(unitName = "carga-uy")
    private EntityManager entityManager;

    @Override
    public GuiaDto getGuiaById(Long id) {
        Guia guia = entityManager.find(Guia.class, id);
        return guia == null ? null : Serializers.toDto(guia);
    }

    @Override
    public GuiaDto getGuiaByNro(String nroGuia) {
        List<Guia> guias = entityManager.createQuery(
                        "SELECT g FROM Guia g WHERE g.nroGuia = :nro", Guia.class)
                .setParameter("nro", nroGuia)
                .getResultList();
        if (guias.isEmpty()) {
            return null;
        }
        return Serializers.toDto(guias.get(0));
    }

    @Override
    public List<GuiaDto> getAll() {
        return toDtos(entityManager.createQuery("SELECT g FROM Guia g ORDER BY g.fecha DESC", Guia.class).getResultList());
    }

    @Override
    public List<GuiaDto> getByEmpresa(Long idEmpresa) {
        return toDtos(entityManager.createQuery(
                        "SELECT g FROM Guia g WHERE g.empresa.id = :idEmpresa ORDER BY g.fecha DESC", Guia.class)
                .setParameter("idEmpresa", idEmpresa)
                .getResultList());
    }

    @Override
    public GuiaDto addGuia(GuiaDto guiaDto) {
        Guia guia = Serializers.toEntity(guiaDto,
                entityManager.find(Empresa.class, guiaDto.getIdEmpresa()),
                entityManager.find(Ciudadano.class, guiaDto.getIdRegistradaPor()),
                entityManager.find(Rubro.class, guiaDto.getIdRubro()),
                entityManager.find(TipoCarga.class, guiaDto.getIdTipoCarga()));
        entityManager.persist(guia);
        return Serializers.toDto(guia);
    }

    @Override
    public void updateGuia(Long id, GuiaDto guiaDto) {
        Guia guia = entityManager.find(Guia.class, id);
        if (guia != null) {
            guia.setNroGuia(guiaDto.getNroGuia());
            guia.setFecha(guiaDto.getFecha());
            guia.setOrigen(guiaDto.getOrigen());
            guia.setDestino(guiaDto.getDestino());
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
