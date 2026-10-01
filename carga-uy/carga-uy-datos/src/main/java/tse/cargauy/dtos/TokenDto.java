package tse.cargauy.dtos;

import java.io.Serializable;

public class TokenDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String token;
    private final long segundosVigencia;

    public TokenDto(String token, long segundosVigencia) {
        this.token = token;
        this.segundosVigencia = segundosVigencia;
    }

    public String getToken() {
        return token;
    }

    public long getSegundosVigencia() {
        return segundosVigencia;
    }
}
