package tse.cargauy.nodotracking;

import java.time.Instant;

import jakarta.json.Json;

// un evento de posicion; el id lo genera el GPS, asi un reenvio del mismo evento se reconoce como duplicado
public record EventoGps(String idEvento, String matricula, double latitud, double longitud, Instant timestampGeneracion) {

    // mismo formato que lee IngestaTrackingMDB en el central
    public String aMensaje(String nodoId) {
        return Json.createObjectBuilder()
                .add("idEvento", idEvento)
                .add("nodo", nodoId)
                .add("matricula", matricula)
                .add("latitud", latitud)
                .add("longitud", longitud)
                .add("timestampGeneracion", timestampGeneracion.toString())
                .build()
                .toString();
    }
}
