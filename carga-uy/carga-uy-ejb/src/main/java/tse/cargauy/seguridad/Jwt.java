package tse.cargauy.seguridad;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

// implementacion sobre JJWT: jwt firmado con HS256 (HMAC-SHA256 con el secreto) con iss, sub, typ, iat y exp
public final class Jwt {

    public static final String ISSUER = "carga-uy";
    private static final String CLAIM_TIPO = "typ";

    private Jwt() {
    }

    // JJWT rechaza claves de menos de 256 bits con WeakKeyException (RFC 7518 3.2)
    public static SecretKey clave(String secreto) {
        return Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    public static String firmar(String sujeto, String tipo, Map<String, ?> claims, Duration duracion, SecretKey clave) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .claims(claims)
                .issuer(ISSUER)
                .subject(sujeto)
                .claim(CLAIM_TIPO, tipo)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(duracion)))
                // HS256 fijo: sin el algoritmo, JJWT elige HS384 o HS512 segun el largo de la clave
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    // devuelve null si esta mal formado, no esta firmado o la firma no coincide, el iss o el typ no son los
    // esperados, no tiene exp o ya vencio
    public static Claims verificar(String jwt, String tipo, SecretKey clave) {
        try {
            // parseSignedClaims rechaza los tokens sin firma (alg none)
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .requireIssuer(ISSUER)
                    .require(CLAIM_TIPO, tipo)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();
            // JJWT controla exp solo si viene, y aca es obligatorio
            return claims.getExpiration() == null ? null : claims;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
