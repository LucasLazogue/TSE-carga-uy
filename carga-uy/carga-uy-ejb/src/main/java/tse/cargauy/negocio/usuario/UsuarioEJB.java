package tse.cargauy.negocio.usuario;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import tse.cargauy.adaptadores.gubuy.GubUyClient;
import tse.cargauy.adaptadores.gubuy.IdentidadGubUy;
import tse.cargauy.data.usuario.UsuarioDAOLocal;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.seguridad.Jwt;

@Stateless
public class UsuarioEJB implements UsuarioEJBLocal {

    private static final boolean MOCK = "true".equals(System.getenv("GUBUY_MOCK"));
    private static final String ISSUER = "carga-uy";
    private static final Duration DURACION_SESION = Duration.ofHours(6);

    @EJB
    UsuarioDAOLocal usuarioDAO;

    @Inject
    GubUyClient gubUy;

    public URI getGubUyLoginUrl(String state, String nonce) {
        return gubUy.getAuthorizationUrl(state, nonce);
    }

    public UsuarioDto loginGubUy(String code, String nonce) {
        IdentidadGubUy identidad = gubUy.exchangeCode(code, nonce);
        return ingresar(identidad.getCedula(), identidad.getCorreo());
    }

    // solo con GUBUY_MOCK=true, mientras no haya credenciales de gub.uy: entra con la cedula sin autenticar
    public UsuarioDto loginPrueba(String cedula) {
        if (!MOCK) {
            throw new CargaUYException(CodigoError.AUTH_SOLICITUD_INVALIDA);
        }
        return ingresar(cedula, null);
    }

    // la sesion viaja en el token (la web en una cookie HttpOnly, mobile como Bearer), el servidor no guarda estado
    public String crearToken(UsuarioDto usuario) {
        Instant ahora = Instant.now();
        JsonObjectBuilder claims = Json.createObjectBuilder()
                .add("iss", ISSUER)
                .add("sub", String.valueOf(usuario.getId()))
                .add("cedula", usuario.getCedula())
                .add("roles", Json.createArrayBuilder(usuario.getRoles()))
                .add("iat", ahora.getEpochSecond())
                .add("exp", ahora.plus(DURACION_SESION).getEpochSecond());
        if (usuario.getCorreo() != null) {
            claims.add("correo", usuario.getCorreo());
        }
        return Jwt.firmar(claims.build(), secreto());
    }

    public UsuarioDto validarToken(String token) {
        JsonObject claims = Jwt.verificar(token, secreto());
        if (claims == null || !ISSUER.equals(claims.getString("iss", null))) {
            throw new CargaUYException(CodigoError.AUTH_SESION_INVALIDA);
        }
        UsuarioDto usuario = new UsuarioDto();
        usuario.setId(Long.valueOf(claims.getString("sub")));
        usuario.setCedula(claims.getString("cedula"));
        usuario.setCorreo(claims.getString("correo", null));
        usuario.setRoles(claims.getJsonArray("roles").getValuesAs(JsonString::getString));
        return usuario;
    }

    private UsuarioDto ingresar(String cedula, String correo) {
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
        return usuarioDAO.addCiudadano(cedula, correo);
    }

    private static String secreto() {
        String secreto = System.getenv("CARGAUY_JWT_SECRET");
        if (secreto == null || secreto.isBlank()) {
            throw new CargaUYException(CodigoError.AUTH_SECRETO_NO_CONFIGURADO);
        }
        return secreto;
    }

}
