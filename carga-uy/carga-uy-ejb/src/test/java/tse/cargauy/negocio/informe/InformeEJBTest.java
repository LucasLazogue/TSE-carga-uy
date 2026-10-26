package tse.cargauy.negocio.informe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.data.informe.InformeDAO;
import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.Granularidad;
import tse.cargauy.dtos.IndicadoresDto;
import tse.cargauy.dtos.InformeDto;
import tse.cargauy.dtos.InfraccionesPorTipoDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.ViajesPorPeriodoDto;
import tse.cargauy.dtos.VolumenPorRubroDto;
import tse.cargauy.entities.EstadoInforme;
import tse.cargauy.entities.FormatoInforme;
import tse.cargauy.entities.TipoInfraccion;
import tse.cargauy.entities.TipoInforme;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

class InformeEJBTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 26);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 26, 10, 0);

    private final List<ViajesPorPeriodoDto> viajesPorDia = new ArrayList<>();
    private final List<InfraccionesPorTipoDto> infracciones = new ArrayList<>();
    private LocalDate fechaHabilitados;
    private final List<InformeDto> registrados = new ArrayList<>();
    private Long solicitanteListado;

    private final InformeEJB ejb = new InformeEJB() {
        @Override
        Long idUsuario() {
            return 9L;
        }

        @Override
        LocalDate hoy() {
            return HOY;
        }

        @Override
        LocalDateTime ahora() {
            return AHORA;
        }
    };

    @BeforeEach
    void iniciar() {
        ejb.informeDAO = new InformeDAO() {
            @Override
            public long contarVehiculosHabilitados(LocalDate fecha) {
                fechaHabilitados = fecha;
                return 4;
            }

            @Override
            public long contarEmpresasHabilitadas(LocalDate fecha) {
                return 3;
            }

            @Override
            public List<ViajesPorPeriodoDto> contarViajesPorDia(FiltroInforme filtro) {
                return viajesPorDia;
            }

            @Override
            public List<VolumenPorRubroDto> sumarVolumenPorRubro(FiltroInforme filtro) {
                return List.of();
            }

            @Override
            public List<InfraccionesPorTipoDto> contarInfracciones(FiltroInforme filtro) {
                return infracciones;
            }

            @Override
            public InformeDto addInforme(InformeDto informe) {
                informe.setId(1L);
                registrados.add(informe);
                return informe;
            }

            @Override
            public PaginaDto<InformeDto> getBySolicitante(Long idSolicitante, Paginacion paginacion) {
                solicitanteListado = idSolicitante;
                return new PaginaDto<>(List.of(), 0, paginacion.getPagina(), paginacion.getTamanio());
            }
        };
    }

    private IndicadoresDto consultar(LocalDate desde, LocalDate hasta) {
        return ejb.consultar(new FiltroInforme(desde, hasta, null));
    }

    private static void rechaza(CodigoError codigo, FiltroInforme filtro, InformeEJB ejb) {
        assertEquals(codigo, assertThrows(CargaUYException.class, () -> ejb.consultar(filtro)).getCodigo());
    }

    @Test
    void elPeriodoEsObligatorio() {
        rechaza(CodigoError.INFORME_PERIODO_REQUERIDO, new FiltroInforme(null, HOY, null), ejb);
        rechaza(CodigoError.INFORME_PERIODO_REQUERIDO, new FiltroInforme(HOY, null, null), ejb);
    }

    @Test
    void elFinNoPuedeSerAnteriorAlInicio() {
        rechaza(CodigoError.INFORME_RANGO_FECHAS_INVALIDO, new FiltroInforme(HOY, HOY.minusDays(1), null), ejb);
    }

    @Test
    void elPeriodoNoPuedeSuperarCincoAnios() {
        LocalDate desde = LocalDate.of(2021, 10, 26);

        assertEquals(Granularidad.MES, consultar(desde, HOY).getGranularidad());
        rechaza(CodigoError.INFORME_PERIODO_DEMASIADO_LARGO, new FiltroInforme(desde, HOY.plusDays(1), null), ejb);
    }

    @Test
    void masDeDoceMesesEsUnPeriodoAmplio() {
        LocalDate desde = LocalDate.of(2025, 10, 26);

        assertFalse(consultar(desde, HOY).isPeriodoAmplio());
        assertTrue(consultar(desde.minusDays(1), HOY).isPeriodoAmplio());
    }

    @Test
    void laGranularidadDependeDelLargoDelPeriodo() {
        LocalDate desde = LocalDate.of(2026, 1, 10);

        assertEquals(Granularidad.DIA, consultar(desde, LocalDate.of(2026, 3, 10)).getGranularidad());
        assertEquals(Granularidad.SEMANA, consultar(desde, LocalDate.of(2026, 3, 11)).getGranularidad());
        assertEquals(Granularidad.SEMANA, consultar(desde, LocalDate.of(2026, 7, 10)).getGranularidad());
        assertEquals(Granularidad.MES, consultar(desde, LocalDate.of(2026, 7, 11)).getGranularidad());
    }

    @Test
    void losViajesSeSumanPorSemanaDesdeElLunes() {
        viajesPorDia.add(new ViajesPorPeriodoDto(LocalDate.of(2026, 10, 18), 1));
        viajesPorDia.add(new ViajesPorPeriodoDto(LocalDate.of(2026, 10, 19), 2));
        viajesPorDia.add(new ViajesPorPeriodoDto(LocalDate.of(2026, 10, 25), 3));

        List<ViajesPorPeriodoDto> viajes = consultar(LocalDate.of(2026, 6, 1), HOY).getViajes();

        assertEquals(2, viajes.size());
        assertEquals(LocalDate.of(2026, 10, 12), viajes.get(0).getInicio());
        assertEquals(1, viajes.get(0).getCantidad());
        assertEquals(LocalDate.of(2026, 10, 19), viajes.get(1).getInicio());
        assertEquals(5, viajes.get(1).getCantidad());
    }

    @Test
    void losViajesSeSumanPorMes() {
        viajesPorDia.add(new ViajesPorPeriodoDto(LocalDate.of(2026, 9, 30), 1));
        viajesPorDia.add(new ViajesPorPeriodoDto(LocalDate.of(2026, 10, 1), 2));
        viajesPorDia.add(new ViajesPorPeriodoDto(LocalDate.of(2026, 10, 25), 3));

        List<ViajesPorPeriodoDto> viajes = consultar(LocalDate.of(2026, 1, 1), HOY).getViajes();

        assertEquals(2, viajes.size());
        assertEquals(LocalDate.of(2026, 9, 1), viajes.get(0).getInicio());
        assertEquals(1, viajes.get(0).getCantidad());
        assertEquals(LocalDate.of(2026, 10, 1), viajes.get(1).getInicio());
        assertEquals(5, viajes.get(1).getCantidad());
    }

    @Test
    void todosLosTiposDeInfraccionAparecenAunqueNoTenganCasos() {
        infracciones.add(new InfraccionesPorTipoDto(TipoInfraccion.EXCESO_CARGA, 6, 3, 1));

        IndicadoresDto indicadores = consultar(LocalDate.of(2026, 10, 1), HOY);

        assertEquals(TipoInfraccion.values().length, indicadores.getInfracciones().size());
        InfraccionesPorTipoDto exceso = indicadores.getInfracciones().stream()
                .filter(i -> i.getTipo() == TipoInfraccion.EXCESO_CARGA).findFirst().orElseThrow();
        assertEquals(2, exceso.getPendientes());
        assertEquals(0, indicadores.getInfracciones().stream()
                .filter(i -> i.getTipo() == TipoInfraccion.DESVIO_RUTA).findFirst().orElseThrow().getDetectadas());
    }

    @Test
    void laTasaEsConfirmadasSobreDetectadas() {
        infracciones.add(new InfraccionesPorTipoDto(TipoInfraccion.EXCESO_CARGA, 6, 3, 1));
        infracciones.add(new InfraccionesPorTipoDto(TipoInfraccion.DESVIO_RUTA, 2, 1, 0));

        IndicadoresDto indicadores = consultar(LocalDate.of(2026, 10, 1), HOY);

        assertEquals(8, indicadores.getDetectadas());
        assertEquals(4, indicadores.getConfirmadas());
        assertEquals(3, indicadores.getPendientes());
        assertEquals(0.5, indicadores.getTasaConfirmadas());
    }

    @Test
    void sinCasosNoHayTasaYSeInformaQueNoHayDatos() {
        IndicadoresDto indicadores = consultar(LocalDate.of(2026, 10, 1), HOY);

        assertNull(indicadores.getTasaConfirmadas());
        assertTrue(indicadores.isSinDatos());
    }

    @Test
    void losHabilitadosSeCuentanAlFinDelPeriodoOHoySiTodaviaNoTermino() {
        consultar(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
        assertEquals(LocalDate.of(2026, 3, 31), fechaHabilitados);

        consultar(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31));
        assertEquals(HOY, fechaHabilitados);
    }

    @Test
    void exportarRegistraLaSolicitudPendienteDelUsuario() {
        Long idTipoCarga = 2L;

        ejb.exportar(new FiltroInforme(LocalDate.of(2026, 1, 1), HOY, idTipoCarga), TipoInforme.VOLUMEN_POR_RUBRO, FormatoInforme.CSV);

        assertEquals(1, registrados.size());
        InformeDto informe = registrados.get(0);
        assertEquals(TipoInforme.VOLUMEN_POR_RUBRO, informe.getTipo());
        assertEquals(FormatoInforme.CSV, informe.getFormato());
        assertEquals(EstadoInforme.PENDIENTE, informe.getEstado());
        assertEquals(LocalDate.of(2026, 1, 1), informe.getPeriodoDesde());
        assertEquals(HOY, informe.getPeriodoHasta());
        assertEquals(idTipoCarga, informe.getIdTipoCarga());
        assertEquals(9L, informe.getIdSolicitante());
        assertEquals(AHORA, informe.getFechaSolicitud());
    }

    @Test
    void exportarValidaElPeriodo() {
        assertThrows(CargaUYException.class,
                () -> ejb.exportar(new FiltroInforme(HOY, HOY.minusDays(1), null), TipoInforme.HABILITADOS, FormatoInforme.PDF));
        assertTrue(registrados.isEmpty());
    }

    @Test
    void misExportacionesSonLasDelUsuario() {
        ejb.getMisInformes(new Paginacion(0, 10));

        assertEquals(9L, solicitanteListado);
    }
}
