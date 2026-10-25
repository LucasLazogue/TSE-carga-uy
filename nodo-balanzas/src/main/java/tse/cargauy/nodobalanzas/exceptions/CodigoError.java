package tse.cargauy.nodobalanzas.exceptions;

public enum CodigoError {

    PESAJE_FECHA_REQUERIDA(1001, Categoria.VALIDACION),
    PESAJE_MATRICULA_O_RUT(1002, Categoria.VALIDACION),

    AUTH_CLAVE_INVALIDA(9001, Categoria.NO_AUTENTICADO),
    ACCESO_DENEGADO(9002, Categoria.PROHIBIDO);

    private final int codigo;
    private final Categoria categoria;

    CodigoError(int codigo, Categoria categoria) {
        this.codigo = codigo;
        this.categoria = categoria;
    }

    public int getCodigo() {
        return codigo;
    }

    public Categoria getCategoria() {
        return categoria;
    }
}
