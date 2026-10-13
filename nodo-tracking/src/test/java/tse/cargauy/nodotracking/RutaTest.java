package tse.cargauy.nodotracking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RutaTest {

    private static final Punto MONTEVIDEO = new Punto(-34.9011, -56.1645);
    private static final Punto RIVERA = new Punto(-30.9053, -55.5508);
    private final Ruta ruta = new Ruta(MONTEVIDEO, RIVERA);

    @Test
    void distanciaMontevideoRivera() {
        // con double nunca se compara por igualdad exacta: el tercer parametro es la tolerancia
        assertEquals(448, Geo.distanciaKm(MONTEVIDEO, RIVERA), 1);
    }

    @Test
    void enElKilometroCeroEstaEnElOrigen() {
        Punto punto = ruta.puntoEn(0, 0);

        assertEquals(MONTEVIDEO.lat(), punto.lat(), 1e-9);
        assertEquals(MONTEVIDEO.lon(), punto.lon(), 1e-9);
    }

    @Test
    void alFinalEstaEnElDestinoAunqueSePase() {
        Punto punto = ruta.puntoEn(ruta.largoKm() + 50, 0);

        assertEquals(RIVERA.lat(), punto.lat(), 1e-9);
        assertEquals(RIVERA.lon(), punto.lon(), 1e-9);
    }

    @Test
    void elDesvioCorreElPuntoLaDistanciaPedida() {
        double mitad = ruta.largoKm() / 2;

        double distancia = Geo.distanciaKm(ruta.puntoEn(mitad, 0), ruta.puntoEn(mitad, 15));

        assertEquals(15, distancia, 0.5);
    }

    @Test
    void invertidaVaDelDestinoAlOrigen() {
        assertEquals(RIVERA, ruta.invertida().origen());
        assertEquals(MONTEVIDEO, ruta.invertida().destino());
    }
}
