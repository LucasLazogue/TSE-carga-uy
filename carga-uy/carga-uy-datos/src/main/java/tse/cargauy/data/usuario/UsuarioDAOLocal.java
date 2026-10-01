package tse.cargauy.data.usuario;

import java.util.List;
import jakarta.ejb.Local;
import tse.cargauy.dtos.UsuarioDto;

@Local
public interface UsuarioDAOLocal {
    UsuarioDto getById(Long id);
    UsuarioDto getCiudadanoByCedula(String cedula);
    UsuarioDto getFuncionarioByCedula(String cedula);
    UsuarioDto addCiudadano(String cedula, String correo);
    void addFuncionario(String cedula);
    void addChofer(String cedula, String correo);
    List<String> getRoles();
    void addRol(String nombre);
}
