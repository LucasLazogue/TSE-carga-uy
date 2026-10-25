package tse.cargauy.dtos;

import java.io.Serializable;

// refreshToken es null al renovar: no se rota
public class TokensMovilDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String accessToken;
    private final long segundosAcceso;
    private final String refreshToken;
    private final long segundosRefresh;

    public TokensMovilDto(String accessToken, long segundosAcceso, String refreshToken, long segundosRefresh) {
        this.accessToken = accessToken;
        this.segundosAcceso = segundosAcceso;
        this.refreshToken = refreshToken;
        this.segundosRefresh = segundosRefresh;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public long getSegundosAcceso() {
        return segundosAcceso;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    // contando la inactividad
    public long getSegundosRefresh() {
        return segundosRefresh;
    }
}
