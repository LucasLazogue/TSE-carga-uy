package tse.cargauy.negocio.usuario;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import tse.cargauy.adaptadores.gubuy.GubUyClient;
import tse.cargauy.adaptadores.gubuy.IdentidadGubUy;
import tse.cargauy.data.usuario.UsuarioDAOLocal;
import tse.cargauy.dtos.EstadoLoginDto;
import tse.cargauy.dtos.TokenDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.seguridad.Jwt;

@Stateless
public class UsuarioEJB implements UsuarioEJBLocal {

    // HS256 necesita una clave de al menos 256 bits (RFC 7518 3.2)
    private static final int LARGO_MINIMO_SECRETO = 32;
    private static final Duration DURACION_SESION = Duration.ofHours(6);
    private static final Duration DURACION_CODIGO_MOBILE = Duration.ofMinutes(1);
    private static final Duration DURACION_LOGIN = Duration.ofMinutes(5);
    private static final String TIPO_SESION = "sesion";
    private static final String TIPO_CODIGO_MOBILE = "codigo_mobile";
    private static final String TIPO_LOGIN = "login";
    private static final String CODE_CHALLENGE = "cc";
    private static final String STATE = "state";
    private static final String NONCE = "nonce";

    @EJB
    UsuarioDAOLocal usuarioDAO;

    @Inject
    GubUyClient gubUy;

    public boolean isLoginMock() { // TODO mock: borrar
        return gubUy.isMock();
    }

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

    public TokenDto crearEstadoLogin(String state, String nonce, String codeChallenge) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(STATE, state);
        claims.put(NONCE, nonce);
        if (codeChallenge != null) {
            claims.put(CODE_CHALLENGE, codeChallenge);
        }
        // todavia no hay usuario, por eso no lleva sub
        String token = Jwt.firmar(null, TIPO_LOGIN, claims, DURACION_LOGIN, clave());
        return new TokenDto(token, DURACION_LOGIN.toSeconds());
    }

    public EstadoLoginDto leerEstadoLogin(String token) {
        Claims claims = verificar(token, TIPO_LOGIN, CodigoError.AUTH_SOLICITUD_INVALIDA);
        return new EstadoLoginDto(claims.get(STATE, String.class), claims.get(NONCE, String.class),
                claims.get(CODE_CHALLENGE, String.class));
    }

    public TokenDto crearToken(UsuarioDto usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("cedula", usuario.getCedula());
        claims.put("roles", usuario.getRoles());
        if (usuario.getCorreo() != null) {
            claims.put("correo", usuario.getCorreo());
        }
        String token = Jwt.firmar(String.valueOf(usuario.getId()), TIPO_SESION, claims, DURACION_SESION, clave());
        return new TokenDto(token, DURACION_SESION.toSeconds());
    }

    public UsuarioDto validarToken(String token) {
        return usuario(verificar(token, TIPO_SESION, CodigoError.AUTH_SESION_INVALIDA));
    }

    public String crearCodigoMobile(UsuarioDto usuario, String codeChallenge) {
        return Jwt.firmar(String.valueOf(usuario.getId()), TIPO_CODIGO_MOBILE, Map.of(CODE_CHALLENGE, codeChallenge),
                DURACION_CODIGO_MOBILE, clave());
    }

    public TokenDto canjearCodigoMobile(String codigo, String codeVerifier) {
        Claims claims = verificar(codigo, TIPO_CODIGO_MOBILE, CodigoError.AUTH_SOLICITUD_INVALIDA);
        String codeChallenge = claims.get(CODE_CHALLENGE, String.class);
        if (codeVerifier == null || codeChallenge == null || !MessageDigest.isEqual(
                codeChallenge.getBytes(StandardCharsets.US_ASCII),
                challenge(codeVerifier).getBytes(StandardCharsets.US_ASCII))) {
            throw new CargaUYException(CodigoError.AUTH_SOLICITUD_INVALIDA);
        }
        UsuarioDto usuario = usuarioDAO.getById(Long.valueOf(claims.getSubject()));
        if (usuario == null) {
            throw new CargaUYException(CodigoError.AUTH_SOLICITUD_INVALIDA);
        }
        return crearToken(usuario);
    }

    private Claims verificar(String token, String tipo, CodigoError error) {
        Claims claims = Jwt.verificar(token, tipo, clave());
        if (claims == null) {
            throw new CargaUYException(error);
        }
        return claims;
    }

    private static UsuarioDto usuario(Claims claims) {
        UsuarioDto usuario = new UsuarioDto();
        usuario.setId(Long.valueOf(claims.getSubject()));
        usuario.setCedula(claims.get("cedula", String.class));
        usuario.setCorreo(claims.get("correo", String.class));
        // el json se lee como lista sin tipo; los roles siempre se firman como textos
        List<?> roles = claims.get("roles", List.class);
        usuario.setRoles(roles.stream().map(String::valueOf).toList());
        return usuario;
    }

    private SecretKey clave() {
        String secreto = leerSecreto();
        if (secreto == null || secreto.isBlank()) {
            throw new CargaUYException(CodigoError.AUTH_SECRETO_NO_CONFIGURADO);
        }
        if (secreto.getBytes(StandardCharsets.UTF_8).length < LARGO_MINIMO_SECRETO) {
            throw new CargaUYException(CodigoError.AUTH_SECRETO_CORTO, String.valueOf(LARGO_MINIMO_SECRETO));
        }
        return Jwt.clave(secreto);
    }

    // aparte para poder reemplazarlo en los tests
    String leerSecreto() {
        return System.getenv("CARGAUY_JWT_SECRET");
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
