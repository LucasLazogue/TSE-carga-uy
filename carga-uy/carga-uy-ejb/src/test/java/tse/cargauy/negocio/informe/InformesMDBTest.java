package tse.cargauy.negocio.informe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.negocio.MensajesJms;

class InformesMDBTest {

    private final InformesMDB mdb = new InformesMDB();
    private final List<Long> generados = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mdb.generador = generados::add;
    }

    @Test
    void generaElInformeDelMensaje() {
        mdb.onMessage(MensajesJms.texto("42"));

        assertEquals(List.of(42L), generados);
    }

    // un mensaje mal formado se descarta: reintentarlo no lo arregla
    @Test
    void descartaLosMensajesMalFormados() {
        mdb.onMessage(MensajesJms.texto("cuarenta y dos"));
        mdb.onMessage(MensajesJms.noTexto());
        mdb.onMessage(MensajesJms.ilegible());

        assertTrue(generados.isEmpty());
    }
}
