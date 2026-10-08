package tse.cargauy.nodotracking;

final class Geo {

    static final double RADIO_TIERRA_KM = 6371;
    static final double KM_POR_GRADO = 111.0;

    private Geo() {
    }

    // haversine: distancia sobre la superficie de la tierra
    static double distanciaKm(Punto a, Punto b) {
        double dLat = Math.toRadians(b.lat() - a.lat());
        double dLon = Math.toRadians(b.lon() - a.lon());
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(a.lat())) * Math.cos(Math.toRadians(b.lat())) * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * RADIO_TIERRA_KM * Math.asin(Math.sqrt(h));
    }
}
