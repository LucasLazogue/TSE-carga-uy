package tse.cargauy.negocio.usuario;

import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.dtos.UsuarioDto;

@Local
public interface FuncionarioEJBLocal {
    List<UsuarioDto> getFuncionarios();
    void addFuncionario(String cedula);
    void deleteFuncionario(String cedula);
}
