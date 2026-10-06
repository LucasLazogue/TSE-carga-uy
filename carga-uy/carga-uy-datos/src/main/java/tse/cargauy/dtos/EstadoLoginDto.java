package tse.cargauy.dtos;

import java.io.Serializable;

// lo que hay que recordar entre /login y /callback; codeChallenge es null en el login web
public class EstadoLoginDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String state;
    private final String nonce;
    private final String codeChallenge;

    public EstadoLoginDto(String state, String nonce, String codeChallenge) {
        this.state = state;
        this.nonce = nonce;
        this.codeChallenge = codeChallenge;
    }

    public String getState() {
        return state;
    }

    public String getNonce() {
        return nonce;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }
}
