package tse.cargauy.data.empresa;

import java.time.LocalDate;
import java.util.List;
import jakarta.ejb.Local;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;

@Local
public interface EmpresaDAOLocal {
    EmpresaDto getEmpresaById(Long id);
    EmpresaDto getEmpresaByNro(int nroEmpresa);
    PaginaDto<EmpresaDto> getAll(Paginacion paginacion);
    List<EmpresaDto> findByNombre(String nombre);
    List<EmpresaDto> getByResponsable(Long idCiudadano, LocalDate fecha);
    EmpresaDto addEmpresa(EmpresaDto empresaDto);
    void updateEmpresa(Long id, EmpresaDto empresaDto);
    void deleteEmpresa(Long id);
}
