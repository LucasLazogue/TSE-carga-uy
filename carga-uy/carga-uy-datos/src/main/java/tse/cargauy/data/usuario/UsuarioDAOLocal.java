package tse.cargauy.data.usuario;

import java.time.LocalDate;
import java.util.List;
import jakarta.ejb.Local;
import tse.cargauy.dtos.ChoferDto;
import tse.cargauy.dtos.FiltroChoferes;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.UsuarioDto;

@Local
public interface UsuarioDAOLocal {
    UsuarioDto getById(Long id);
    UsuarioDto getCiudadanoByCedula(String cedula);
    UsuarioDto getFuncionarioByCedula(String cedula);
    UsuarioDto addCiudadano(String cedula, String correo);
    void updateCorreo(Long idCiudadano, String correo);
    void addFuncionario(String cedula);
    PaginaDto<UsuarioDto> getFuncionarios(Paginacion paginacion);
    void deleteFuncionario(String cedula);
    void deleteUsuarioBackoffice(String usuario);
    UsuarioDto saveUsuarioBackoffice(String usuario, List<String> roles);
    ChoferDto addChofer(Long idCiudadano, Long idEmpresa, LocalDate fechaDesde);
    void addResponsable(Long idCiudadano, Long idEmpresa, LocalDate fechaDesde);
    Long getChoferVigente(Long idCiudadano, Long idEmpresa, LocalDate fecha);
    Long getResponsableVigente(Long idCiudadano, Long idEmpresa, LocalDate fecha);
    Long getChoferSinBaja(Long idCiudadano, Long idEmpresa, LocalDate fecha);
    ChoferDto getVinculoSinBaja(Long idVinculo, Long idEmpresa);
    ChoferDto darDeBajaChofer(Long idVinculo, LocalDate fecha);
    PaginaDto<ChoferDto> getChoferes(Long idEmpresa, FiltroChoferes filtro, Paginacion paginacion);
    List<String> getRoles();
    void addRol(String nombre);
}
