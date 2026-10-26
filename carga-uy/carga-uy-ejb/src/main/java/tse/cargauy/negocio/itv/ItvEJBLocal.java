package tse.cargauy.negocio.itv;

import java.util.List;
import jakarta.ejb.Local;
import tse.cargauy.dtos.ItvDto;

@Local
public interface ItvEJBLocal {
    List<ItvDto> listar(Long idVehiculo);
    void agregar(Long idVehiculo, ItvDto itv);
    void eliminar(Long idVehiculo, Long id);
}
