package tse.cargauy.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.PublicJwk;

class DpopTest {

    private static final String URL = "http://192.168.1.50:8080/carga-uy/api/auth/token";

    private final KeyPair celular = Jwts.SIG.ES256.keyPair().build();
    private final KeyPair otro = Jwts.SIG.ES256.keyPair().build();
    private final Instant ahora = Instant.now();

    private static PublicJwk<?> jwk(KeyPair par) {
        return Jwks.builder().key((ECPublicKey) par.getPublic()).build();
    }

    private String prueba(KeyPair firma, KeyPair enHeader, String tipo, String metodo, String url, Instant iat) {
        return Jwts.builder()
                .header().type(tipo).jwk(jwk(enHeader)).and()
                .id(UUID.randomUUID().toString())
                .claim("htm", metodo)
                .claim("htu", url)
                .issuedAt(Date.from(iat))
                .signWith(firma.getPrivate(), Jwts.SIG.ES256)
                .compact();
    }

    private String valida() {
        return prueba(celular, celular, "dpop+jwt", "POST", URL, ahora);
    }

    @Test
    void unaPruebaValidaDevuelveLaHuellaDeLaClave() {
        assertEquals(jwk(celular).thumbprint().toString(), Dpop.verificar(valida(), "POST", URL, ahora));
    }

    @Test
    void cadaClaveTieneSuHuella() {
        String otraPrueba = prueba(otro, otro, "dpop+jwt", "POST", URL, ahora);

        assertNotEquals(Dpop.verificar(valida(), "POST", URL, ahora), Dpop.verificar(otraPrueba, "POST", URL, ahora));
    }

    @Test
    void rechazaUnaFirmaQueNoEsDeLaClaveDelHeader() {
        // alguien que copio la clave publica pero no tiene la privada
        String copiada = prueba(otro, celular, "dpop+jwt", "POST", URL, ahora);

        assertNull(Dpop.verificar(copiada, "POST", URL, ahora));
    }

    @Test
    void rechazaOtroMetodoOtraUrlOtroTipo() {
        assertNull(Dpop.verificar(valida(), "GET", URL, ahora));
        assertNull(Dpop.verificar(valida(), "POST", "http://192.168.1.50:8080/carga-uy/api/movil/sync", ahora));
        assertNull(Dpop.verificar(prueba(celular, celular, "JWT", "POST", URL, ahora), "POST", URL, ahora));
    }

    @Test
    void rechazaUnaPruebaFueraDeLaVentana() {
        String vieja = prueba(celular, celular, "dpop+jwt", "POST", URL, ahora.minusSeconds(61));
        String delFuturo = prueba(celular, celular, "dpop+jwt", "POST", URL, ahora.plusSeconds(61));

        assertNull(Dpop.verificar(vieja, "POST", URL, ahora));
        assertNull(Dpop.verificar(delFuturo, "POST", URL, ahora));
        // un reloj algo atrasado o adelantado se tolera
        assertEquals(Dpop.verificar(valida(), "POST", URL, ahora),
                Dpop.verificar(prueba(celular, celular, "dpop+jwt", "POST", URL, ahora.minusSeconds(30)), "POST", URL, ahora));
    }

    @Test
    void rechazaSinPruebaOMalFormada() {
        assertNull(Dpop.verificar(null, "POST", URL, ahora));
        assertNull(Dpop.verificar("no-es-un-jwt", "POST", URL, ahora));
    }

    @Test
    void comparaLasUrlsComoUris() {
        assertTrue(Dpop.mismaUri("HTTPS://Carga.uy/api/auth/token", "https://carga.uy:443/api/auth/token"));
        assertTrue(Dpop.mismaUri("http://carga.uy/api/auth/token", "http://carga.uy:80/api/auth/token"));
        assertFalse(Dpop.mismaUri("http://carga.uy/api/auth/token", "https://carga.uy/api/auth/token"));
        assertFalse(Dpop.mismaUri("http://carga.uy:8080/api/auth/token", "http://carga.uy:8081/api/auth/token"));
    }
}
