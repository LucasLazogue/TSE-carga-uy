package tse.cargauy.nodobalanzas.exceptions;

public enum Categoria {

    VALIDACION(400),
    NO_AUTENTICADO(401),
    PROHIBIDO(403),
    NO_ENCONTRADO(404),
    CONFLICTO(409);

    private final int status;

    Categoria(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
