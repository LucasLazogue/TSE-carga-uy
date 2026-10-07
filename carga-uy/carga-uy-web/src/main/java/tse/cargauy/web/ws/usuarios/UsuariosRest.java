package tse.cargauy.web.ws.usuarios;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import tse.cargauy.dtos.UsuarioActualDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.usuario.UsuarioEJBLocal;

@RequestScoped
@Path("/usuarios")
@Produces(MediaType.APPLICATION_JSON)
public class UsuariosRest {

    @EJB
    UsuarioEJBLocal usuarioEJB;

    @GET
    @Path("/me")
    public UsuarioActualDto getUsuarioActual(@Context SecurityContext seguridad) {
        if (seguridad.getUserPrincipal() == null) {
            throw new CargaUYException(CodigoError.AUTH_SESION_INVALIDA);
        }
        return usuarioEJB.getUsuarioActual();
    }
}
