package tse.cargauy.pdisuplantador;

// los codigos de la seccion 2.4 de la descripcion funcional del servicio que este suplantador simula
enum CodigoDnic {

    PERSONA_INEXISTENTE(500, "Persona inexistente"),
    DATOS_A_REGULARIZAR(701, "Datos de Persona a regularizar"),
    CONSULTA_INCOMPLETA(1001, "No se pudo completar la consulta"),
    CEDULA_ANULADA(1003, "Numero de cedula anulado"),
    PARAMETROS_INCORRECTOS(10001, "Parametros incorrectos"),
    ACCESO_NO_AUTORIZADO(10002, "Acceso No Autorizado");

    private final int codigo;
    private final String descripcion;

    CodigoDnic(int codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    int getCodigo() {
        return codigo;
    }

    String getDescripcion() {
        return descripcion;
    }
}
