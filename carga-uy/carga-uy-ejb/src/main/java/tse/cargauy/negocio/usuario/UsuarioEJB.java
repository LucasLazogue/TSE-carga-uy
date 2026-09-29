package tse.cargauy.negocio.usuario;

import java.net.URI;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import tse.cargauy.adaptadores.gubuy.GubUyClient;
import tse.cargauy.adaptadores.gubuy.IdentidadGubUy;
import tse.cargauy.data.usuario.UsuarioDAOLocal;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

@Stateless
public class UsuarioEJB implements UsuarioEJBLocal {

    @EJB
    UsuarioDAOLocal usuarioDAO;

    @Inject
    GubUyClient gubUy;

    public URI getGubUyLoginUrl(String state, String nonce) {
        return gubUy.getAuthorizationUrl(state, nonce);
    }

    public UsuarioDto loginGubUy(String code, String nonce) {
        IdentidadGubUy identidad = gubUy.exchangeCode(code, nonce);
        String cedula = identidad.getCedula();
        if (cedula == null || cedula.isBlank()) {
            throw new CargaUYException(CodigoError.USUARIO_CEDULA_REQUERIDA);
        }

        UsuarioDto funcionario = usuarioDAO.getFuncionarioByCedula(cedula);
        if (funcionario != null) {
            return funcionario;
        }

        UsuarioDto ciudadano = usuarioDAO.getCiudadanoByCedula(cedula);
        if (ciudadano != null) {
            return ciudadano;
        }
        return usuarioDAO.addCiudadano(cedula, identidad.getCorreo());
    }

}
