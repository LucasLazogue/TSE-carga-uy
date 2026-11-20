package tse.cargauy.negocio.empresa;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.MensajesJms;

class EmpresaQueueMDBTest {

    private final EmpresaQueueMDB mdb = new EmpresaQueueMDB();
    private final List<EmpresaDto> altas = new ArrayList<>();

    private RuntimeException falla;

    @BeforeEach
    void setUp() throws Exception {
        EmpresaEJBLocal empresaEJB = (EmpresaEJBLocal) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { EmpresaEJBLocal.class }, (proxy, metodo, args) -> {
                    if (metodo.getName().equals("addEmpresa")) {
                        altas.add((EmpresaDto) args[0]);
                        if (falla != null) {
                            throw falla;
                        }
                        return args[0];
                    }
                    return null;
                });
        Field campo = EmpresaQueueMDB.class.getDeclaredField("empresaEJB");
        campo.setAccessible(true);
        campo.set(mdb, empresaEJB);
    }

    @Test
    void daDeAltaLaEmpresaDelMensaje() {
        mdb.onMessage(MensajesJms.texto(" 123 |Transportes Sur|Transportes Sur S.A.|Ruta 5 km 30| 11111111 "));

        assertEquals(1, altas.size());
        EmpresaDto empresa = altas.get(0);
        assertEquals(123, empresa.getNroEmpresa());
        assertEquals("Transportes Sur", empresa.getNombrePublico());
        assertEquals("Transportes Sur S.A.", empresa.getRazonSocial());
        assertEquals("Ruta 5 km 30", empresa.getDireccionPrincipal());
        assertEquals("11111111", empresa.getCedulaResponsable());
    }

    // ninguno de estos se arregla reintentando: se descartan sin excepcion para que no se reentreguen
    @Test
    void descartaLosMensajesInvalidos() {
        mdb.onMessage(MensajesJms.texto("123|Transportes Sur|faltan campos"));
        mdb.onMessage(MensajesJms.texto("abc|Transportes Sur|Transportes Sur S.A.|Ruta 5|11111111"));
        mdb.onMessage(MensajesJms.noTexto());
        mdb.onMessage(MensajesJms.ilegible());

        assertTrue(altas.isEmpty());
    }

    @Test
    void unRechazoDelNegocioNoSePropaga() {
        falla = new CargaUYException(CodigoError.ACCESO_DENEGADO);

        assertDoesNotThrow(() -> mdb.onMessage(MensajesJms.texto("123|Transportes Sur|S.A.|Ruta 5|11111111")));
        assertEquals(1, altas.size());
    }
}
