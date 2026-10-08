package tse.cargauy.negocio.empresa;

import java.util.List;
import jakarta.ejb.Local;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;

@Local
public interface EmpresaEJBLocal {
    EmpresaDto getEmpresaById(Long id);
    PaginaDto<EmpresaDto> getAll(Paginacion paginacion);
    PaginaDto<EmpresaDto> findByNombre(String nombre, Paginacion paginacion);
    void validarAcceso(Long idEmpresa);
    EmpresaDto addEmpresa(EmpresaDto empresaDto);
    void updateEmpresa(Long id, EmpresaDto empresaDto);
    void deleteEmpresa(Long id);
}
