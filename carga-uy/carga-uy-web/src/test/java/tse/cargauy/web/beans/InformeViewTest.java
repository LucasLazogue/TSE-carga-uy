package tse.cargauy.web.beans;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.primefaces.model.StreamedContent;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.Granularidad;
import tse.cargauy.dtos.IndicadoresDto;
import tse.cargauy.dtos.InfraccionesPorTipoDto;
import tse.cargauy.dtos.InformeDto;
import tse.cargauy.dtos.TipoCargaDto;
import tse.cargauy.dtos.ViajesPorPeriodoDto;
import tse.cargauy.dtos.VolumenPorRubroDto;
import tse.cargauy.entities.EstadoInforme;
import tse.cargauy.entities.FormatoInforme;
import tse.cargauy.entities.TipoInfraccion;
import tse.cargauy.negocio.guia.GuiaEJBLocal;
import tse.cargauy.negocio.informe.InformeEJBLocal;

// la pantalla de reportes del backoffice: indicadores, graficos (Chart.js) y descarga de informes
class InformeViewTest {

    private final InformeEJBLocal informes = mock(InformeEJBLocal.class);
    private final GuiaEJBLocal guias = mock(GuiaEJBLocal.class);
    private final InformeView vista = new InformeView();
    private FacesContext contexto;

    @BeforeEach
    void preparar() throws ReflectiveOperationException {
        contexto = ContextoFaces.usar(rol -> true);
        poner("informeEJB", informes);
        poner("guiaEJB", guias);
        vista.init();
    }

    @AfterEach
    void soltar() {
        ContextoFaces.soltar();
    }

    private void poner(String campo, Object valor) throws ReflectiveOperationException {
        Field f = InformeView.class.getDeclaredField(campo);
        f.setAccessible(true);
        f.set(vista, valor);
    }

    private static JsonObject json(String texto) {
        return Json.createReader(new StringReader(texto)).readObject();
    }

    private IndicadoresDto indicadores() {
        IndicadoresDto indicadores = new IndicadoresDto();
        indicadores.setGranularidad(Granularidad.DIA);
        indicadores.setHabilitadosAl(LocalDate.of(2026, 11, 5));
        indicadores.setViajes(List.of(new ViajesPorPeriodoDto(LocalDate.of(2026, 11, 1), 4),
                new ViajesPorPeriodoDto(LocalDate.of(2026, 11, 2), 7)));
        indicadores.setVolumenPorRubro(List.of(new VolumenPorRubroDto("Granos", 30000)));
        indicadores.setInfracciones(List.of(new InfraccionesPorTipoDto(TipoInfraccion.EXCESO_CARGA, 5, 2, 1)));
        return indicadores;
    }

    // por defecto consulta desde el primero de enero hasta hoy
    @Test
    void arrancaConElAnioEnCurso() {
        assertEquals(LocalDate.now(), vista.getHasta());
        assertEquals(LocalDate.now().withDayOfYear(1), vista.getDesde());
    }

    @Test
    void consultaConElFiltroDeLaPantalla() {
        IndicadoresDto indicadores = indicadores();
        when(informes.consultar(any())).thenReturn(indicadores);
        vista.setDesde(LocalDate.of(2026, 1, 1));
        vista.setHasta(LocalDate.of(2026, 6, 30));
        vista.setIdTipoCarga(4L);

        vista.consultar();

        ArgumentCaptor<FiltroInforme> filtro = ArgumentCaptor.forClass(FiltroInforme.class);
        verify(informes).consultar(filtro.capture());
        assertEquals(LocalDate.of(2026, 1, 1), filtro.getValue().getDesde());
        assertEquals(LocalDate.of(2026, 6, 30), filtro.getValue().getHasta());
        assertEquals(4L, filtro.getValue().getIdTipoCarga());
        assertEquals(4L, vista.getIdTipoCarga());
        assertSame(indicadores, vista.getIndicadores());
        assertEquals("05/11/2026", vista.getHabilitadosAl());
    }

    @Test
    void graficoDeViajesPorPeriodo() {
        when(informes.consultar(any())).thenReturn(indicadores());
        vista.consultar();

        JsonObject grafico = json(vista.getGraficoViajes());

        assertEquals("line", grafico.getString("type"));
        JsonObject datos = grafico.getJsonObject("data");
        assertEquals(2, datos.getJsonArray("labels").size());
        assertEquals("01/11/2026", datos.getJsonArray("labels").getString(0));
        JsonArray serie = datos.getJsonArray("datasets").getJsonObject(0).getJsonArray("data");
        assertEquals(4, serie.getInt(0));
        assertEquals(7, serie.getInt(1));
        assertFalse(grafico.getJsonObject("options").getBoolean("maintainAspectRatio"));
    }

    @Test
    void graficoDeVolumenPorRubro() {
        when(informes.consultar(any())).thenReturn(indicadores());
        vista.consultar();

        JsonObject grafico = json(vista.getGraficoVolumen());

        assertEquals("bar", grafico.getString("type"));
        assertEquals("y", grafico.getJsonObject("options").getString("indexAxis"));
        assertEquals("Granos", grafico.getJsonObject("data").getJsonArray("labels").getString(0));
    }

    // infracciones apiladas: confirmadas, desestimadas y pendientes por tipo
    @Test
    void graficoDeInfraccionesApiladas() {
        when(informes.consultar(any())).thenReturn(indicadores());
        vista.consultar();

        JsonObject grafico = json(vista.getGraficoInfracciones());

        JsonObject datos = grafico.getJsonObject("data");
        assertEquals("Exceso de carga", datos.getJsonArray("labels").getString(0));
        JsonArray series = datos.getJsonArray("datasets");
        assertEquals(3, series.size());
        assertEquals(2, series.getJsonObject(0).getJsonArray("data").getInt(0));
        assertEquals(1, series.getJsonObject(1).getJsonArray("data").getInt(0));
        assertTrue(grafico.getJsonObject("options").getJsonObject("scales").getJsonObject("x").getBoolean("stacked"));
    }

    @Test
    void exportarPideElInformeYAvisaQueSeEstaGenerando() {
        vista.exportar(FormatoInforme.PDF);

        verify(informes).exportar(any(FiltroInforme.class), eq(FormatoInforme.PDF));
        verify(contexto).addMessage(eq(null), any(FacesMessage.class));
    }

    @Test
    void descargaElArchivoConSuNombreYTipo() throws Exception {
        InformeDto informe = new InformeDto();
        informe.setId(9L);
        informe.setPeriodoDesde(LocalDate.of(2026, 1, 1));
        informe.setPeriodoHasta(LocalDate.of(2026, 6, 30));
        informe.setFormato(FormatoInforme.PDF);
        when(informes.descargar(9L)).thenReturn(new byte[] { 1, 2, 3 });

        StreamedContent pdf = vista.archivo(informe);
        informe.setFormato(FormatoInforme.CSV);
        StreamedContent csv = vista.archivo(informe);

        assertEquals("reporte_2026-01-01_2026-06-30.pdf", pdf.getName());
        assertEquals("application/pdf", pdf.getContentType());
        try (InputStream contenido = pdf.getStream().get()) {
            assertArrayEquals(new byte[] { 1, 2, 3 }, contenido.readAllBytes());
        }
        assertEquals("reporte_2026-01-01_2026-06-30.csv", csv.getName());
        assertEquals("text/csv; charset=UTF-8", csv.getContentType());
    }

    @Test
    void severidadDeCadaEstado() {
        InformeDto informe = new InformeDto();
        String[] esperadas = { "info", "success", "danger", "secondary" };
        EstadoInforme[] estados = { EstadoInforme.PENDIENTE, EstadoInforme.GENERADO, EstadoInforme.FALLIDO, EstadoInforme.VENCIDO };
        for (int i = 0; i < estados.length; i++) {
            informe.setEstado(estados[i]);
            assertEquals(esperadas[i], vista.severidad(informe));
        }
    }

    // los tipos de carga se piden una sola vez por pedido
    @Test
    void tiposDeCargaYPendientes() {
        when(guias.getTiposCarga()).thenReturn(List.of(new TipoCargaDto()));
        when(informes.tienePendientes()).thenReturn(true);

        vista.getTiposCarga();
        vista.getTiposCarga();

        verify(guias, times(1)).getTiposCarga();
        assertTrue(vista.isTienePendientes());
        assertSame(vista.getInformes(), vista.getInformes());
    }
}
