package tse.cargauy.adaptadores.balanzas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import tse.cargauy.adaptadores.balanzas.CortePorNodo.Estado;

class CortePorNodoTest {

    private long ahora = 1_000_000;
    private final CortePorNodo corte = new CortePorNodo(() -> ahora);

    private void llamadas(String nodo, int exitos, int fallas) {
        for (int i = 0; i < exitos; i++) {
            corte.exito(nodo);
        }
        for (int i = 0; i < fallas; i++) {
            corte.falla(nodo);
        }
    }

    @Test
    void unNodoCaidoNoCortaAlOtro() {
        llamadas("balanzas-norte", 0, 10);
        llamadas("balanzas-sur", 10, 0);

        assertFalse(corte.permite("balanzas-norte"));
        assertTrue(corte.permite("balanzas-sur"));
        assertEquals(Estado.ABIERTO, corte.estado("balanzas-norte"));
        assertEquals(Estado.CERRADO, corte.estado("balanzas-sur"));
    }

    @Test
    void seAbreConCincoFallasEnLasUltimasDiez() {
        llamadas("nodo", 5, 4);
        assertTrue(corte.permite("nodo"));

        corte.falla("nodo");

        assertFalse(corte.permite("nodo"));
    }

    @Test
    void conMenosDeDiezLlamadasNoSeAbre() {
        llamadas("nodo", 0, 9);

        assertTrue(corte.permite("nodo"));
    }

    @Test
    void lasFallasViejasSalenDeLaVentana() {
        llamadas("nodo", 0, 4);
        llamadas("nodo", 10, 0);
        llamadas("nodo", 0, 4);

        assertTrue(corte.permite("nodo"));
        assertEquals(Estado.CERRADO, corte.estado("nodo"));
    }

    @Test
    void alPasarLosSesentaSegundosDejaPasarUnaSolaPrueba() {
        llamadas("nodo", 0, 10);
        ahora += CortePorNodo.ABIERTO_MS - 1;
        assertFalse(corte.permite("nodo"));

        ahora += 1;

        assertTrue(corte.permite("nodo"));
        assertEquals(Estado.MEDIO_ABIERTO, corte.estado("nodo"));
        assertFalse(corte.permite("nodo"));
    }

    @Test
    void siLaPruebaAndaSeCierra() {
        llamadas("nodo", 0, 10);
        ahora += CortePorNodo.ABIERTO_MS;
        corte.permite("nodo");

        corte.exito("nodo");

        assertEquals(Estado.CERRADO, corte.estado("nodo"));
        assertTrue(corte.permite("nodo"));
        // vuelve a empezar la cuenta: hacen falta otras 10 llamadas para abrirlo
        llamadas("nodo", 0, 9);
        assertTrue(corte.permite("nodo"));
    }

    @Test
    void siLaPruebaFallaVuelveAAbrirseOtrosSesentaSegundos() {
        llamadas("nodo", 0, 10);
        ahora += CortePorNodo.ABIERTO_MS;
        corte.permite("nodo");

        corte.falla("nodo");

        assertEquals(Estado.ABIERTO, corte.estado("nodo"));
        ahora += CortePorNodo.ABIERTO_MS - 1;
        assertFalse(corte.permite("nodo"));
    }

    @Test
    void unaPruebaQueNoInformaNoDejaElNodoCortadoParaSiempre() {
        llamadas("nodo", 0, 10);
        ahora += CortePorNodo.ABIERTO_MS;
        assertTrue(corte.permite("nodo"));

        ahora += CortePorNodo.ABIERTO_MS;

        assertTrue(corte.permite("nodo"));
    }

    @Test
    void soloConoceLosNodosConsultados() {
        assertFalse(corte.conoce("nodo"));

        corte.permite("nodo");

        assertTrue(corte.conoce("nodo"));
    }
}
