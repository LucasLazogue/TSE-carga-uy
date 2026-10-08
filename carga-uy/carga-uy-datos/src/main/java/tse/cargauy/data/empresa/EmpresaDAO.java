package tse.cargauy.data.empresa;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.entities.Empresa;

@Stateless
public class EmpresaDAO implements EmpresaDAOLocal {

    @PersistenceContext(unitName = "carga-uy")
    private EntityManager entityManager;

    @Override
    public EmpresaDto getEmpresaById(Long id) {
        Empresa empresa = entityManager.find(Empresa.class, id);
        if (empresa != null) {
            return Serializers.toDto(empresa);
        }
        return null;
    }

    @Override
    public EmpresaDto getEmpresaByNro(int nroEmpresa) {
        List<Empresa> empresas = entityManager.createQuery(
                        "SELECT e FROM Empresa e WHERE e.nroEmpresa = :nro", Empresa.class)
                .setParameter("nro", nroEmpresa)
                .getResultList();
        if (empresas.isEmpty()) {
            return null;
        }
        return Serializers.toDto(empresas.get(0));
    }

    @Override
    public PaginaDto<EmpresaDto> getAll(Paginacion paginacion) {
        List<EmpresaDto> res = new ArrayList<>();

        List<Empresa> empresas = entityManager.createQuery("SELECT e FROM Empresa e ORDER BY e.nroEmpresa", Empresa.class)
                .setFirstResult(paginacion.getDesde())
                .setMaxResults(paginacion.getTamanio())
                .getResultList();
        for (Empresa e : empresas) {
            res.add(Serializers.toDto(e));
        }
        long total = entityManager.createQuery("SELECT COUNT(e) FROM Empresa e", Long.class).getSingleResult();
        return new PaginaDto<>(res, total, paginacion.getPagina(), paginacion.getTamanio());
    }

    @Override
    public List<EmpresaDto> findByNombre(String nombre) {
        List<EmpresaDto> res = new ArrayList<>();

        List<Empresa> empresas = entityManager.createQuery(
                        "SELECT e FROM Empresa e WHERE LOWER(e.nombrePublico) LIKE :nombre OR LOWER(e.razonSocial) LIKE :nombre ORDER BY e.nroEmpresa", Empresa.class)
                .setParameter("nombre", "%" + nombre.toLowerCase() + "%")
                .getResultList();
        for (Empresa e : empresas) {
            res.add(Serializers.toDto(e));
        }
        return res;
    }

    @Override
    public List<EmpresaDto> getByResponsable(Long idCiudadano, LocalDate fecha) {
        return entityManager.createQuery(
                        "SELECT r.empresa FROM Responsable r WHERE r.ciudadano.id = :idCiudadano"
                                + " AND r.fechaDesde <= :fecha AND (r.fechaHasta IS NULL OR r.fechaHasta >= :fecha)"
                                + " ORDER BY r.empresa.nroEmpresa", Empresa.class)
                .setParameter("idCiudadano", idCiudadano)
                .setParameter("fecha", fecha)
                .getResultList()
                .stream()
                .map(Serializers::toDto)
                .toList();
    }

    @Override
    public EmpresaDto addEmpresa(EmpresaDto empresaDto) {
        Empresa empresa = Serializers.toEntity(empresaDto);
        entityManager.persist(empresa);
        return Serializers.toDto(empresa);
    }

    @Override
    public void updateEmpresa(Long id, EmpresaDto empresaDto) {
        Empresa empresa = entityManager.find(Empresa.class, id);
        if (empresa != null) {
            empresa.setNroEmpresa(empresaDto.getNroEmpresa());
            empresa.setNombrePublico(empresaDto.getNombrePublico());
            empresa.setRazonSocial(empresaDto.getRazonSocial());
            empresa.setDireccionPrincipal(empresaDto.getDireccionPrincipal());
        }
    }

    @Override
    public void deleteEmpresa(Long id) {
        Empresa empresa = entityManager.find(Empresa.class, id);
        if (empresa != null) {
            entityManager.remove(empresa);
        }
    }
}
