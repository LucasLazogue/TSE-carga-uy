package tse.cargauy.nodotracking;

// tramo en linea recta: en las distancias de Uruguay se aparta poco de la geodesica, y coincide con el criterio de
// desvio propuesto (distancia al segmento origen-destino de la Guia)
public record Ruta(Punto origen, Punto destino) {

    public double largoKm() {
        return Geo.distanciaKm(origen, destino);
    }

    public Ruta invertida() {
        return new Ruta(destino, origen);
    }

    // el punto a 'km' del origen, corrido 'desvioKm' hacia la izquierda de la ruta (0 = sobre la ruta)
    public Punto puntoEn(double km, double desvioKm) {
        double f = Math.min(1, km / largoKm()); // el ultimo tick puede pasarse: el camion queda en el destino
        double lat = origen.lat() + (destino.lat() - origen.lat()) * f;
        double lon = origen.lon() + (destino.lon() - origen.lon()) * f;
        if (desvioKm == 0) {
            return new Punto(lat, lon);
        }
        // se pasa todo a km para que la perpendicular sea perpendicular de verdad: un grado de longitud mide
        // menos que uno de latitud (a la latitud de Uruguay, unos 92 km contra 111)
        double kmPorGradoLon = Geo.KM_POR_GRADO * Math.cos(Math.toRadians(lat));
        double norte = (destino.lat() - origen.lat()) * Geo.KM_POR_GRADO;
        double este = (destino.lon() - origen.lon()) * kmPorGradoLon;
        double largo = Math.hypot(norte, este);
        double norteDesvio = este / largo * desvioKm;
        double esteDesvio = -norte / largo * desvioKm;
        return new Punto(lat + norteDesvio / Geo.KM_POR_GRADO, lon + esteDesvio / kmPorGradoLon);
    }
}
