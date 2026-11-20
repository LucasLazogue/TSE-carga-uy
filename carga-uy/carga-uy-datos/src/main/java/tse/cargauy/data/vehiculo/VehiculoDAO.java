package tse.cargauy.data.vehiculo;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tse.cargauy.dtos.FiltroVehiculos;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.entities.Empresa;
import tse.cargauy.entities.Vehiculo;

@Stateless
public class VehiculoDAO implements VehiculoDAOLocal {

    @PersistenceContext(unitName = "carga-uy")
    private EntityManager entityManager;

    @Override
    public VehiculoDto getVehiculoById(Long id) {
        Vehiculo vehiculo = entityManager.find(Vehiculo.class, id);
        if (vehiculo != null) {
            return Serializers.toDto(vehiculo);
        }
        return null;
    }

    @Override
    public VehiculoDto getVehiculoByMatricula(String matricula) {
        List<Vehiculo> vehiculos = entityManager.createQuery(
                        "SELECT v FROM Vehiculo v WHERE v.matricula = :matricula", Vehiculo.class)
                .setParameter("matricula", matricula)
                .getResultList();
        if (vehiculos.isEmpty()) {
            return null;
        }
        return Serializers.toDto(vehiculos.get(0));
    }

    @Override
    public PaginaDto<VehiculoDto> getAll(Paginacion paginacion) {
        List<VehiculoDto> res = new ArrayList<>();

        List<Vehiculo> vehiculos = entityManager.createQuery("SELECT v FROM Vehiculo v ORDER BY v.matricula", Vehiculo.class)
                .setFirstResult(paginacion.getDesde())
                .setMaxResults(paginacion.getTamanio())
                .getResultList();
        for (Vehiculo v : vehiculos) {
            res.add(Serializers.toDto(v));
        }
        long total = entityManager.createQuery("SELECT COUNT(v) FROM Vehiculo v", Long.class).getSingleResult();
        return new PaginaDto<>(res, total, paginacion.getPagina(), paginacion.getTamanio());
    }

    @Override
    public PaginaDto<VehiculoDto> getByEmpresa(FiltroVehiculos filtro, Paginacion paginacion) {
        List<VehiculoDto> res = new ArrayList<>();
        int minimo = filtro.getCapacidadMinima() == null ? 0 : filtro.getCapacidadMinima();

        List<Vehiculo> vehiculos = entityManager.createQuery(
                        "SELECT v FROM Vehiculo v WHERE v.empresa.id = :idEmpresa AND v.capacidadCarga >= :capacidadMinima"
                                + " ORDER BY v.matricula", Vehiculo.class)
                .setParameter("idEmpresa", filtro.getIdEmpresa())
                .setParameter("capacidadMinima", minimo)
                .setFirstResult(paginacion.getDesde())
                .setMaxResults(paginacion.getTamanio())
                .getResultList();
        for (Vehiculo v : vehiculos) {
            res.add(Serializers.toDto(v));
        }
        long total = entityManager.createQuery(
                        "SELECT COUNT(v) FROM Vehiculo v WHERE v.empresa.id = :idEmpresa AND v.capacidadCarga >= :capacidadMinima",
                        Long.class)
                .setParameter("idEmpresa", filtro.getIdEmpresa())
                .setParameter("capacidadMinima", minimo)
                .getSingleResult();
        return new PaginaDto<>(res, total, paginacion.getPagina(), paginacion.getTamanio());
    }

    @Override
    public void addVehiculo(VehiculoDto vehiculoDto) {
        Empresa empresa = entityManager.find(Empresa.class, vehiculoDto.getIdEmpresa());
        Vehiculo vehiculo = Serializers.toEntity(vehiculoDto, empresa);
        entityManager.persist(vehiculo);
    }

    @Override
    public void updateVehiculo(Long id, VehiculoDto vehiculoDto) {
        Vehiculo vehiculo = entityManager.find(Vehiculo.class, id);
        if (vehiculo != null) {
            vehiculo.setMatricula(vehiculoDto.getMatricula());
            vehiculo.setMarca(vehiculoDto.getMarca());
            vehiculo.setModelo(vehiculoDto.getModelo());
            vehiculo.setPesoVehiculo(vehiculoDto.getPesoVehiculo());
            vehiculo.setCapacidadCarga(vehiculoDto.getCapacidadCarga());
            vehiculo.setEmpresa(entityManager.find(Empresa.class, vehiculoDto.getIdEmpresa()));
        }
    }

    @Override
    public void deleteVehiculo(Long id) {
        Vehiculo vehiculo = entityManager.find(Vehiculo.class, id);
        if (vehiculo != null) {
            entityManager.remove(vehiculo);
        }
    }
}
