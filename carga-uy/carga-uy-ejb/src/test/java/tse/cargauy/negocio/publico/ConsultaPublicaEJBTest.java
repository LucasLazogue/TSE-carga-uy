package tse.cargauy.negocio.publico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import tse.cargauy.data.empresa.EmpresaDAO;
import tse.cargauy.data.informe.InformeDAO;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.HabilitacionEmpresaDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

class ConsultaPublicaEJBTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 14);

    private final List<EmpresaDto> empresas = new ArrayList<>();
    private final Set<Long> habilitadas = new HashSet<>();
    private final List<LocalDate> fechasConsultadas = new ArrayList<>();
    private final ConsultaPublicaEJB ejb = new ConsultaPublicaEJB() {
        @Override
        LocalDate hoy() {
            return HOY;
        }
    };

    @BeforeEach
    void iniciar() {
        ejb.empresaDAO = new EmpresaDAO() {
            @Override
            public EmpresaDto getEmpresaByNro(int nroEmpresa) {
                return empresas.stream().filter(e -> e.getNroEmpresa() == nroEmpresa).findFirst().orElse(null);
            }

            @Override
            public List<EmpresaDto> getByRazonSocial(String razonSocial, int maximo) {
                return empresas.stream()
                        .filter(e -> e.getRazonSocial().trim().equalsIgnoreCase(razonSocial.trim()))
                        .limit(maximo)
                        .toList();
            }
        };
        ejb.informeDAO = new InformeDAO() {
            @Override
            public boolean isEmpresaHabilitada(Long idEmpresa, LocalDate fecha) {
                fechasConsultadas.add(fecha);
                return habilitadas.contains(idEmpresa);
            }
        };
        empresa(1L, 1001, "Transportes del Sur", "Transportes del Sur S.A.");
        empresa(2L, 1002, "Cargas Norte", "Cargas del Norte S.R.L.");
        habilitadas.add(1L);
    }

    private void empresa(Long id, int nro, String nombrePublico, String razonSocial) {
        EmpresaDto empresa = new EmpresaDto(nro, nombrePublico, razonSocial, "Av. Italia 3456", null);
        empresa.setId(id);
        empresas.add(empresa);
    }

    private static void assertCodigo(CodigoError esperado, Executable accion) {
        assertEquals(esperado, assertThrows(CargaUYException.class, accion).getCodigo());
    }

    @Test
    void porNumeroDevuelveLaEmpresaHabilitadaALaFechaDeHoy() {
        HabilitacionEmpresaDto habilitacion = ejb.validarEmpresa(1001, null);

        assertEquals("Transportes del Sur", habilitacion.getNombrePublico());
        assertEquals("Transportes del Sur S.A.", habilitacion.getRazonSocial());
        assertTrue(habilitacion.isHabilitada());
        assertEquals(HOY, habilitacion.getFecha());
        assertEquals(List.of(HOY), fechasConsultadas);
    }

    @Test
    void empresaSinVehiculosHabilitadosNoEstaHabilitada() {
        assertFalse(ejb.validarEmpresa(1002, null).isHabilitada());
    }

    @Test
    void porRazonSocialSinDistinguirMayusculasNiEspacios() {
        HabilitacionEmpresaDto habilitacion = ejb.validarEmpresa(null, "  cargas DEL norte s.r.l. ");

        assertEquals("Cargas Norte", habilitacion.getNombrePublico());
        assertFalse(habilitacion.isHabilitada());
    }

    @Test
    void numeroInexistente() {
        assertCodigo(CodigoError.EMPRESA_CONSULTA_SIN_RESULTADO, () -> ejb.validarEmpresa(9999, null));
    }

    @Test
    void razonSocialParcialNoEncuentraNada() {
        assertCodigo(CodigoError.EMPRESA_CONSULTA_SIN_RESULTADO, () -> ejb.validarEmpresa(null, "Transportes"));
    }

    @Test
    void razonSocialRepetidaPideConsultarPorNumero() {
        empresa(3L, 1003, "Otra del Sur", "Transportes del Sur S.A.");

        assertCodigo(CodigoError.EMPRESA_CONSULTA_AMBIGUA, () -> ejb.validarEmpresa(null, "Transportes del Sur S.A."));
    }

    @Test
    void sinNingunDato() {
        assertCodigo(CodigoError.EMPRESA_CONSULTA_DATO_REQUERIDO, () -> ejb.validarEmpresa(null, null));
        assertCodigo(CodigoError.EMPRESA_CONSULTA_DATO_REQUERIDO, () -> ejb.validarEmpresa(null, "   "));
    }

    @Test
    void conLosDosDatos() {
        assertCodigo(CodigoError.EMPRESA_CONSULTA_DATO_REQUERIDO,
                () -> ejb.validarEmpresa(1001, "Transportes del Sur S.A."));
    }
}
