package tse.cargauy.data.itv;

import java.time.LocalDate;
import java.util.List;
import jakarta.ejb.Local;
import tse.cargauy.dtos.ItvDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;

@Local
public interface ItvDAOLocal {
    ItvDto getItvById(Long id);
    PaginaDto<ItvDto> getAll(Paginacion paginacion);
    List<ItvDto> getByVehiculo(Long idVehiculo);
    ItvDto getVigente(Long idVehiculo, LocalDate fecha);
    void addItv(ItvDto itvDto);
    void deleteItv(Long id);
}
