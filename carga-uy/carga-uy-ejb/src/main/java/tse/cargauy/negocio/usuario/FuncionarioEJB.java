package tse.cargauy.negocio.usuario;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import tse.cargauy.data.usuario.UsuarioDAOLocal;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.entities.Rol;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

@Stateless
@RolesAllowed(Rol.ADMINISTRADOR)
public class FuncionarioEJB implements FuncionarioEJBLocal {

    @EJB
    UsuarioDAOLocal usuarioDAO;

    public PaginaDto<UsuarioDto> getFuncionarios(Paginacion paginacion) {
        return usuarioDAO.getFuncionarios(paginacion);
    }

    public void addFuncionario(String cedula) {
        if (cedula == null || cedula.isBlank()) {
            throw new CargaUYException(CodigoError.FUNCIONARIO_CEDULA_REQUERIDA);
        }
        if (usuarioDAO.getFuncionarioByCedula(cedula) != null) {
            throw new CargaUYException(CodigoError.FUNCIONARIO_DUPLICADO, cedula);
        }
        usuarioDAO.addFuncionario(cedula);
    }

    public void deleteFuncionario(String cedula) {
        if (usuarioDAO.getFuncionarioByCedula(cedula) == null) {
            throw new CargaUYException(CodigoError.FUNCIONARIO_NO_ENCONTRADO, cedula);
        }
        usuarioDAO.deleteFuncionario(cedula);
    }
}
