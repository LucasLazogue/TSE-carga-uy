package tse.cargauy.negocio.tracking;

import io.opentelemetry.context.Context;

// se dispara cuando terminaron todos los pasos de la ingesta de una posicion; la marca se pone al confirmarse.
// traza: la de la ingesta. Los que observan el evento corren al confirmarse la transaccion, cuando el MDB ya cerro su
// span, y la usan para que lo que hacen despues (la fiscalizacion) siga en la misma traza
public record PosicionProcesada(Long idNodo, String idEvento, Context traza) {
}
