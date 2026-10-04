package tse.cargauy.data.viaje;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.Chofer;
import tse.cargauy.entities.Guia;
import tse.cargauy.entities.Vehiculo;
import tse.cargauy.entities.Viaje;

@Stateless
public class ViajeDAO implements ViajeDAOLocal {

    @PersistenceContext(unitName = "carga-uy")
    private EntityManager entityManager;

    @Override
    public ViajeDto getViajeById(Long id) {
        Viaje viaje = entityManager.find(Viaje.class, id);
        return viaje == null ? null : Serializers.toDto(viaje);
    }

    @Override
    public ViajeDto getByGuia(Long idGuia) {
        List<Viaje> viajes = entityManager.createQuery(
                        "SELECT v FROM Viaje v WHERE v.guia.id = :idGuia", Viaje.class)
                .setParameter("idGuia", idGuia)
                .getResultList();
        if (viajes.isEmpty()) {
            return null;
        }
        return Serializers.toDto(viajes.get(0));
    }

    @Override
    public List<ViajeDto> getByEmpresa(Long idEmpresa) {
        return toDtos(entityManager.createQuery(
                        "SELECT v FROM Viaje v WHERE v.guia.empresa.id = :idEmpresa ORDER BY v.guia.fecha DESC", Viaje.class)
                .setParameter("idEmpresa", idEmpresa)
                .getResultList());
    }

    @Override
    public List<ViajeDto> getByChofer(Long idChofer) {
        return toDtos(entityManager.createQuery(
                        "SELECT v FROM Viaje v WHERE v.chofer.id = :idChofer ORDER BY v.guia.fecha DESC", Viaje.class)
                .setParameter("idChofer", idChofer)
                .getResultList());
    }

    @Override
    public List<ViajeDto> getByVehiculo(Long idVehiculo) {
        return toDtos(entityManager.createQuery(
                        "SELECT v FROM Viaje v WHERE v.vehiculo.id = :idVehiculo ORDER BY v.guia.fecha DESC", Viaje.class)
                .setParameter("idVehiculo", idVehiculo)
                .getResultList());
    }

    @Override
    public ViajeDto addViaje(ViajeDto viajeDto) {
        Guia guia = entityManager.find(Guia.class, viajeDto.getIdGuia());
        Viaje viaje = Serializers.toEntity(guia,
                entityManager.find(Vehiculo.class, viajeDto.getIdVehiculo()),
                entityManager.find(Chofer.class, viajeDto.getIdChofer()));
        entityManager.persist(viaje);
        guia.setViaje(viaje);
        return Serializers.toDto(viaje);
    }

    @Override
    public void updateViaje(Long id, ViajeDto viajeDto) {
        Viaje viaje = entityManager.find(Viaje.class, id);
        if (viaje != null) {
            viaje.setVehiculo(entityManager.find(Vehiculo.class, viajeDto.getIdVehiculo()));
            viaje.setChofer(entityManager.find(Chofer.class, viajeDto.getIdChofer()));
        }
    }

    @Override
    public void deleteViaje(Long id) {
        Viaje viaje = entityManager.find(Viaje.class, id);
        if (viaje != null) {
            viaje.getGuia().setViaje(null);
            entityManager.remove(viaje);
        }
    }

    private List<ViajeDto> toDtos(List<Viaje> viajes) {
        List<ViajeDto> res = new ArrayList<>();
        for (Viaje v : viajes) {
            res.add(Serializers.toDto(v));
        }
        return res;
    }
}
