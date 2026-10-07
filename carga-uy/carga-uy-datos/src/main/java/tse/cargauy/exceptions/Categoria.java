package tse.cargauy.exceptions;

public enum Categoria {

    VALIDACION(400),
    NO_AUTENTICADO(401),
    NO_ENCONTRADO(404),
    CONFLICTO(409),
    CONFIGURACION(500),
    NO_DISPONIBLE(503);

    private final int status;

    Categoria(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
