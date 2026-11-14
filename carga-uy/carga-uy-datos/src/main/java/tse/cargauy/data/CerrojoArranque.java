package tse.cargauy.data;

import jakarta.persistence.EntityManager;

// con varias instancias del central (autoescalado) pueden arrancar dos a la vez: lo que prepara la base al arrancar
// (triggers de auditoria, datos de prueba) toma este cerrojo de Postgres, que se libera al terminar la transaccion.
// La segunda instancia espera a que la primera confirme y despues ve lo que ya quedo hecho
public final class CerrojoArranque {

    static final String SENTENCIA = "SELECT 1 FROM (SELECT pg_advisory_xact_lock(hashtext('carga-uy:arranque'))) AS cerrojo";

    private CerrojoArranque() {
    }

    public static void tomar(EntityManager entityManager) {
        entityManager.createNativeQuery(SENTENCIA).getSingleResult();
    }
}
