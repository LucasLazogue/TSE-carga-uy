package tse.cargauy.seguridad;

import java.net.URI;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.ProtectedHeader;
import io.jsonwebtoken.security.PublicJwk;

// RFC 9449: la firma se verifica con la clave publica que trae la propia prueba, y su huella (RFC 7638)
// identifica al celular
public final class Dpop {

    private static final String TIPO = "dpop+jwt";
    private static final String ALGORITMO = "ES256";
    // tambien hacia adelante, por si el reloj del celular adelanta. sin guardar jti: no hay estado entre nodos
    static final Duration VENTANA = Duration.ofSeconds(60);

    private Dpop() {
    }

    // null si la prueba no sirve
    public static String verificar(String prueba, String metodo, String url, Instant ahora) {
        if (prueba == null || prueba.isBlank()) {
            return null;
        }
        try {
            Jws<Claims> jws = Jwts.parser().keyLocator(Dpop::clavePublica).build().parseSignedClaims(prueba);
            JwsHeader header = jws.getHeader();
            Claims claims = jws.getPayload();
            Date iat = claims.getIssuedAt();
            boolean valida = TIPO.equals(header.getType())
                    && ALGORITMO.equals(header.getAlgorithm())
                    && claims.getId() != null
                    && metodo.equals(claims.get("htm", String.class))
                    && mismaUri(claims.get("htu", String.class), url)
                    && iat != null
                    && Duration.between(iat.toInstant(), ahora).abs().compareTo(VENTANA) <= 0;
            return valida ? header.getJwk().thumbprint().toString() : null;
        } catch (JwtException | IllegalArgumentException | ClassCastException e) {
            return null;
        }
    }

    private static Key clavePublica(Header header) {
        PublicJwk<?> jwk = header instanceof ProtectedHeader protegido ? protegido.getJwk() : null;
        if (jwk == null) {
            throw new MalformedJwtException("la prueba DPoP no trae la clave publica");
        }
        return jwk.toKey();
    }

    // RFC 9449 4.3
    static boolean mismaUri(String htu, String url) {
        if (htu == null || url == null) {
            return false;
        }
        try {
            URI a = URI.create(htu);
            URI b = URI.create(url);
            return a.getScheme().equalsIgnoreCase(b.getScheme())
                    && a.getHost().equalsIgnoreCase(b.getHost())
                    && puerto(a) == puerto(b)
                    && a.getRawPath().equals(b.getRawPath());
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }

    private static int puerto(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
}
