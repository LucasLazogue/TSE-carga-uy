package tse.cargauy.data.vehiculo;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    public List<VehiculoDto> getAll() {
        List<VehiculoDto> res = new ArrayList<>();

        List<Vehiculo> vehiculos = entityManager.createQuery("SELECT v FROM Vehiculo v ORDER BY v.matricula", Vehiculo.class).getResultList();
        for (Vehiculo v : vehiculos) {
            res.add(Serializers.toDto(v));
        }
        return res;
    }

    @Override
    public List<VehiculoDto> getByEmpresa(Long idEmpresa) {
        List<VehiculoDto> res = new ArrayList<>();

        List<Vehiculo> vehiculos = entityManager.createQuery(
                        "SELECT v FROM Vehiculo v WHERE v.empresa.id = :idEmpresa ORDER BY v.matricula", Vehiculo.class)
                .setParameter("idEmpresa", idEmpresa)
                .getResultList();
        for (Vehiculo v : vehiculos) {
            res.add(Serializers.toDto(v));
        }
        return res;
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
