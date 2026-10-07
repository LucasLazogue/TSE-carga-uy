package tse.cargauy.data.usuario;

import java.time.LocalDate;
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
    void addChofer(Long idCiudadano, Long idEmpresa, LocalDate fechaDesde);
    void addResponsable(Long idCiudadano, Long idEmpresa, LocalDate fechaDesde);
    Long getChoferVigente(Long idCiudadano, Long idEmpresa, LocalDate fecha);
    Long getResponsableVigente(Long idCiudadano, Long idEmpresa, LocalDate fecha);
    List<String> getRoles();
    void addRol(String nombre);
}
