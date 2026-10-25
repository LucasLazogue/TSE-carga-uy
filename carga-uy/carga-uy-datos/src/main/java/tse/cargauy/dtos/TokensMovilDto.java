package tse.cargauy.dtos;

import java.io.Serializable;

// lo que responde /auth/token a la app movil. refreshToken es null al renovar: la app sigue usando el que tiene
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

    // cuanto falta para que el refresh token deje de servir, contando la inactividad
    public long getSegundosRefresh() {
        return segundosRefresh;
    }
}
