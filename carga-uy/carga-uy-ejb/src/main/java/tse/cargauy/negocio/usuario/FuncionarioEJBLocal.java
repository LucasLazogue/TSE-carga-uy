package tse.cargauy.negocio.usuario;

import jakarta.ejb.Local;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.UsuarioDto;

@Local
public interface FuncionarioEJBLocal {
    PaginaDto<UsuarioDto> getFuncionarios(Paginacion paginacion);
    void addFuncionario(String cedula);
    void deleteFuncionario(String cedula);
}
