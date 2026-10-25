package tse.cargauy.nodobalanzas.exceptions;

public enum CodigoError {

    PESAJE_FECHA_REQUERIDA(1001, Categoria.VALIDACION),
    PESAJE_MATRICULA_O_RUT(1002, Categoria.VALIDACION),

    PEAJE_CODIGO_REQUERIDO(2001, Categoria.VALIDACION),
    PEAJE_NOMBRE_REQUERIDO(2002, Categoria.VALIDACION),
    PEAJE_RUTA_REQUERIDA(2003, Categoria.VALIDACION),
    PEAJE_KM_INVALIDO(2004, Categoria.VALIDACION),
    PEAJE_CODIGO_DUPLICADO(2005, Categoria.CONFLICTO),
    PEAJE_NO_ENCONTRADO(2006, Categoria.NO_ENCONTRADO),

    BALANZA_CODIGO_REQUERIDO(3001, Categoria.VALIDACION),
    BALANZA_SENTIDO_REQUERIDO(3002, Categoria.VALIDACION),
    BALANZA_CLAVE_CORTA(3003, Categoria.VALIDACION),
    BALANZA_CODIGO_DUPLICADO(3004, Categoria.CONFLICTO),
    BALANZA_CLAVE_EN_USO(3005, Categoria.CONFLICTO),

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
