package tse.cargauy.negocio.tracking;

public enum ResultadoIngesta {
    REGISTRADO,
    DUPLICADO,
    // ya estaba guardada pero sin procesar: se completaron los pasos siguientes
    COMPLETADO,
    NODO_DESCONOCIDO,
    NODO_NO_HABILITADO
}
