package tse.cargauy.negocio.usuario;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

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
import tse.cargauy.dtos.TokenDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.seguridad.Jwt;

@Stateless
public class UsuarioEJB implements UsuarioEJBLocal {

    private static final String ISSUER = "carga-uy";
    private static final Duration DURACION_SESION = Duration.ofHours(6);
    private static final Duration DURACION_CODIGO_MOBILE = Duration.ofMinutes(1);
    private static final String TIPO_SESION = "sesion";
    private static final String TIPO_CODIGO_MOBILE = "codigo_mobile";
    private static final String CODE_CHALLENGE = "cc";

    @EJB
    UsuarioDAOLocal usuarioDAO;

    @Inject
    GubUyClient gubUy;

    public URI getGubUyLoginUrl(String state, String nonce, String cedulaMock) { // TODO mock: sacar cedulaMock
        return gubUy.getAuthorizationUrl(state, nonce, cedulaMock);
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

    public TokenDto crearToken(UsuarioDto usuario) {
        JsonObjectBuilder claims = Json.createObjectBuilder()
                .add("sub", String.valueOf(usuario.getId()))
                .add("cedula", usuario.getCedula())
                .add("roles", Json.createArrayBuilder(usuario.getRoles()));
        if (usuario.getCorreo() != null) {
            claims.add("correo", usuario.getCorreo());
        }
        return new TokenDto(firmar(claims, TIPO_SESION, DURACION_SESION), DURACION_SESION.toSeconds());
    }

    public UsuarioDto validarToken(String token) {
        return usuario(verificar(token, TIPO_SESION, CodigoError.AUTH_SESION_INVALIDA));
    }

    public String crearCodigoMobile(UsuarioDto usuario, String codeChallenge) {
        JsonObjectBuilder claims = Json.createObjectBuilder()
                .add("sub", String.valueOf(usuario.getId()))
                .add(CODE_CHALLENGE, codeChallenge);
        return firmar(claims, TIPO_CODIGO_MOBILE, DURACION_CODIGO_MOBILE);
    }

    public TokenDto canjearCodigoMobile(String codigo, String codeVerifier) {
        JsonObject claims = verificar(codigo, TIPO_CODIGO_MOBILE, CodigoError.AUTH_SOLICITUD_INVALIDA);
        if (codeVerifier == null || !MessageDigest.isEqual(
                claims.getString(CODE_CHALLENGE, "").getBytes(StandardCharsets.US_ASCII),
                challenge(codeVerifier).getBytes(StandardCharsets.US_ASCII))) {
            throw new CargaUYException(CodigoError.AUTH_SOLICITUD_INVALIDA);
        }
        UsuarioDto usuario = usuarioDAO.getById(Long.valueOf(claims.getString("sub")));
        if (usuario == null) {
            throw new CargaUYException(CodigoError.AUTH_SOLICITUD_INVALIDA);
        }
        return crearToken(usuario);
    }

    private String firmar(JsonObjectBuilder claims, String tipo, Duration duracion) {
        Instant ahora = Instant.now();
        claims.add("iss", ISSUER)
                .add("typ", tipo)
                .add("iat", ahora.getEpochSecond())
                .add("exp", ahora.plus(duracion).getEpochSecond());
        return Jwt.firmar(claims.build(), secreto());
    }

    private JsonObject verificar(String token, String tipo, CodigoError error) {
        JsonObject claims = Jwt.verificar(token, secreto());
        if (claims == null || !ISSUER.equals(claims.getString("iss", null)) || !tipo.equals(claims.getString("typ", null))) {
            throw new CargaUYException(error);
        }
        return claims;
    }

    private static UsuarioDto usuario(JsonObject claims) {
        UsuarioDto usuario = new UsuarioDto();
        usuario.setId(Long.valueOf(claims.getString("sub")));
        usuario.setCedula(claims.getString("cedula"));
        usuario.setCorreo(claims.getString("correo", null));
        usuario.setRoles(claims.getJsonArray("roles").getValuesAs(JsonString::getString));
        return usuario;
    }

    private static String secreto() {
        String secreto = System.getenv("CARGAUY_JWT_SECRET");
        if (secreto == null || secreto.isBlank()) {
            throw new CargaUYException(CodigoError.AUTH_SECRETO_NO_CONFIGURADO);
        }
        return secreto;
    }

    private static String challenge(String codeVerifier) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
