package tse.cargauy.negocio.informe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.data.informe.InformeDAO;
import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.Granularidad;
import tse.cargauy.dtos.IndicadoresDto;
import tse.cargauy.dtos.InformeDto;
import tse.cargauy.dtos.InfraccionesPorTipoDto;
import tse.cargauy.dtos.ViajesPorPeriodoDto;
import tse.cargauy.dtos.VolumenPorRubroDto;
import tse.cargauy.entities.EstadoInforme;
import tse.cargauy.entities.FormatoInforme;
import tse.cargauy.entities.TipoInfraccion;

class GeneradorInformesEJBTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 26, 10, 0);

    private final InformeDto informe = new InformeDto();
    private byte[] guardado;
    private boolean fallido;
    private RuntimeException alContarRubros;

    private final GeneradorInformesEJB generador = new GeneradorInformesEJB() {
        @Override
        LocalDateTime ahora() {
            return AHORA;
        }
    };

    @BeforeEach
    void iniciar() {
        informe.setId(1L);
        informe.setFormato(FormatoInforme.CSV);
        informe.setEstado(EstadoInforme.PENDIENTE);
        informe.setPeriodoDesde(LocalDate.of(2026, 10, 1));
        informe.setPeriodoHasta(LocalDate.of(2026, 10, 26));
        generador.informeDAO = new InformeDAO() {
            @Override
            public InformeDto getInformeById(Long id) {
                return informe;
            }

            @Override
            public long contarVehiculosHabilitados(LocalDate fecha) {
                return 4;
            }

            @Override
            public long contarEmpresasHabilitadas(LocalDate fecha) {
                return 3;
            }

            @Override
            public List<ViajesPorPeriodoDto> contarViajesPorDia(FiltroInforme filtro) {
                return List.of();
            }

            @Override
            public List<VolumenPorRubroDto> sumarVolumenPorRubro(FiltroInforme filtro) {
                if (alContarRubros != null) {
                    throw alContarRubros;
                }
                return List.of(new VolumenPorRubroDto("Agropecuario", 18000));
            }

            @Override
            public List<InfraccionesPorTipoDto> contarInfracciones(FiltroInforme filtro) {
                return List.of();
            }

            @Override
            public void guardarContenido(Long id, byte[] contenido, LocalDateTime fecha) {
                guardado = contenido;
            }

            @Override
            public void marcarFallido(Long id) {
                fallido = true;
            }
        };
    }

    @Test
    void generaElArchivoYLoGuarda() {
        generador.generar(1L);

        assertTrue(new String(guardado, StandardCharsets.UTF_8).contains("\"Volumen transportado por rubro\";\"Agropecuario\";\"Volumen (kg)\";\"18000\""));
        assertFalse(fallido);
    }

    @Test
    void enPdfGeneraUnDocumento() {
        informe.setFormato(FormatoInforme.PDF);

        generador.generar(1L);

        assertEquals("%PDF-", new String(guardado, 0, 5, StandardCharsets.US_ASCII));
        assertFalse(fallido);
    }

    @Test
    void siYaNoEstaPendienteNoLoVuelveAGenerar() {
        informe.setEstado(EstadoInforme.GENERADO);

        generador.generar(1L);

        assertNull(guardado);
        assertFalse(fallido);
    }

    @Test
    void siLaGeneracionFallaQuedaFallido() {
        alContarRubros = new IllegalStateException("base caida");

        generador.generar(1L);

        assertNull(guardado);
        assertTrue(fallido);
    }

    @Test
    void cadaSeccionTieneSusColumnas() {
        IndicadoresDto indicadores = new IndicadoresDto();
        indicadores.setHabilitadosAl(LocalDate.of(2026, 10, 26));
        indicadores.setEmpresasHabilitadas(3);
        indicadores.setVehiculosHabilitados(4);
        indicadores.setGranularidad(Granularidad.MES);
        indicadores.setViajes(List.of(new ViajesPorPeriodoDto(LocalDate.of(2026, 9, 1), 7)));
        indicadores.setInfracciones(List.of(new InfraccionesPorTipoDto(TipoInfraccion.EXCESO_CARGA, 8, 3, 1)));

        assertEquals(List.of("26/10/2026", "3", "4"), fila(0, indicadores));
        assertEquals(List.of("09/2026", "7"), fila(2, indicadores));
        assertEquals(List.of("Exceso de carga", "8", "3", "1", "4"), fila(4, indicadores));
        assertEquals(List.of("01/10/2026 al 26/10/2026", "8", "3", "37,5 %"), fila(1, indicadores));
    }

    @Test
    void losEncabezadosSalenDelBundle() {
        assertEquals(List.of("Rubro", "Volumen (kg)"),
                GeneradorInformesEJB.secciones(conFiltro(new IndicadoresDto())).get(3).getTabla().getEncabezados());
    }

    @Test
    void sinCasosLaTasaQuedaVacia() {
        assertEquals(List.of("01/10/2026 al 26/10/2026", "0", "0", ""), fila(1, new IndicadoresDto()));
    }

    private static IndicadoresDto conFiltro(IndicadoresDto indicadores) {
        indicadores.setFiltro(new FiltroInforme(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 26), null));
        return indicadores;
    }

    private static List<String> fila(int seccion, IndicadoresDto indicadores) {
        return List.of(GeneradorInformesEJB.secciones(conFiltro(indicadores)).get(seccion).getTabla().getValores().get(0));
    }
}
