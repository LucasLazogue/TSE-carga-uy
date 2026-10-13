package tse.cargauy.nodotracking;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

// el GPS de un camion: avanza sobre su ruta y en cada tick produce un evento con id propio
public class VehiculoSimulado {

    private final String matricula;
    private final double velocidadKmh;
    private final double desvioKm; // se aplica en el tercio central de la ruta; 0 = sin desvio
    private Ruta ruta;
    private double kmRecorridos;

    public VehiculoSimulado(String matricula, Ruta ruta, double velocidadKmh, double desvioKm) {
        this.matricula = matricula;
        this.ruta = ruta;
        this.velocidadKmh = velocidadKmh;
        this.desvioKm = desvioKm;
    }

    // la marca de tiempo es la hora real aunque se acelere la distancia: si no, quedaria en el futuro
    public EventoGps avanzar(Instant ahora, Duration paso, double acelerar) {
        if (kmRecorridos >= ruta.largoKm()) {
            // flujo continuo: al llegar, el camion emprende la vuelta
            ruta = ruta.invertida();
            kmRecorridos = 0;
        }
        kmRecorridos += velocidadKmh * paso.toMillis() / 3_600_000.0 * acelerar;
        double largo = ruta.largoKm();
        boolean enTercioCentral = kmRecorridos > largo / 3 && kmRecorridos < 2 * largo / 3;
        Punto punto = ruta.puntoEn(kmRecorridos, enTercioCentral ? desvioKm : 0);
        return new EventoGps(UUID.randomUUID().toString(), matricula, punto.lat(), punto.lon(), ahora);
    }

    public String getMatricula() {
        return matricula;
    }
}
