package tse.cargauy.negocio.tracking;

// se dispara cuando terminaron todos los pasos de la ingesta de una posicion; la marca se pone al confirmarse
public record PosicionProcesada(Long idNodo, String idEvento) {
}
