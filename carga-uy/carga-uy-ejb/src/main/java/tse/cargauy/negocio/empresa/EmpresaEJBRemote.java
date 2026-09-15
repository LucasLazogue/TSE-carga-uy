package tse.cargauy.negocio.empresa;

import java.util.List;
import jakarta.ejb.Remote;
import tse.cargauy.dtos.EmpresaDto;

@Remote
public interface EmpresaEJBRemote {
    EmpresaDto getEmpresaById(Long id);
    List<EmpresaDto> getAll();
    List<EmpresaDto> findByNombre(String nombre);
    void addEmpresa(EmpresaDto empresaDto);
    void updateEmpresa(Long id, EmpresaDto empresaDto);
    void deleteEmpresa(Long id);
}
