package tse.cargauy.data.empresa;

import java.time.LocalDate;
import java.util.List;
import jakarta.ejb.Local;
import tse.cargauy.dtos.EmpresaDto;

@Local
public interface EmpresaDAOLocal {
    EmpresaDto getEmpresaById(Long id);
    EmpresaDto getEmpresaByNro(int nroEmpresa);
    List<EmpresaDto> getAll();
    List<EmpresaDto> findByNombre(String nombre);
    List<EmpresaDto> getByResponsable(Long idCiudadano, LocalDate fecha);
    EmpresaDto addEmpresa(EmpresaDto empresaDto);
    void updateEmpresa(Long id, EmpresaDto empresaDto);
    void deleteEmpresa(Long id);
}
