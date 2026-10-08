package tse.cargauy.negocio.empresa;

import java.util.List;
import jakarta.ejb.Remote;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;

@Remote
public interface EmpresaEJBRemote {
    EmpresaDto getEmpresaById(Long id);
    PaginaDto<EmpresaDto> getAll(Paginacion paginacion);
    List<EmpresaDto> findByNombre(String nombre);
    EmpresaDto addEmpresa(EmpresaDto empresaDto);
    void updateEmpresa(Long id, EmpresaDto empresaDto);
    void deleteEmpresa(Long id);
}
