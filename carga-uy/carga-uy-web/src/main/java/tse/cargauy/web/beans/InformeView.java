package tse.cargauy.web.beans;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.IndicadoresDto;
import tse.cargauy.dtos.InformeDto;
import tse.cargauy.dtos.InfraccionesPorTipoDto;
import tse.cargauy.dtos.TipoCargaDto;
import tse.cargauy.dtos.ViajesPorPeriodoDto;
import tse.cargauy.dtos.VolumenPorRubroDto;
import tse.cargauy.entities.FormatoInforme;
import tse.cargauy.negocio.guia.GuiaEJBLocal;
import tse.cargauy.negocio.informe.InformeEJBLocal;
import tse.cargauy.negocio.informe.TextosInforme;

@Named
@RequestScoped
public class InformeView {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String PRIMARIO = "#10b981";
    private static final String CONFIRMADAS = "#ef4444";
    private static final String DESESTIMADAS = "#94a3b8";
    private static final String PENDIENTES = "#f59e0b";

    @EJB
    private InformeEJBLocal informeEJB;

    @EJB
    private GuiaEJBLocal guiaEJB;

    private LocalDate desde;
    private LocalDate hasta;
    private Long idTipoCarga;
    private IndicadoresDto indicadores;
    private List<TipoCargaDto> tiposCarga;
    private final ModeloPaginado<InformeDto> informes = new ModeloPaginado<>(paginacion -> informeEJB.getMisInformes(paginacion));

    @PostConstruct
    public void init() {
        hasta = LocalDate.now();
        desde = hasta.withDayOfYear(1);
    }

    public void consultar() {
        indicadores = informeEJB.consultar(new FiltroInforme(desde, hasta, idTipoCarga));
    }

    public void exportar(FormatoInforme formato) {
        informeEJB.exportar(new FiltroInforme(desde, hasta, idTipoCarga), formato);
        mensaje(TextosInforme.resolver("exportaciones.generando"));
    }

    public String getHabilitadosAl() {
        return indicadores.getHabilitadosAl().format(DIA);
    }

    public String getGraficoViajes() {
        JsonArrayBuilder periodos = Json.createArrayBuilder();
        JsonArrayBuilder viajes = Json.createArrayBuilder();
        for (ViajesPorPeriodoDto periodo : indicadores.getViajes()) {
            periodos.add(indicadores.periodo(periodo));
            viajes.add(periodo.getCantidad());
        }
        return grafico("line", periodos, Json.createArrayBuilder().add(serie("columna.viajes", PRIMARIO, viajes)),
                Json.createObjectBuilder().add("scales", Json.createObjectBuilder()
                        .add("y", Json.createObjectBuilder().add("beginAtZero", true))));
    }

    public String getGraficoVolumen() {
        JsonArrayBuilder rubros = Json.createArrayBuilder();
        JsonArrayBuilder volumen = Json.createArrayBuilder();
        for (VolumenPorRubroDto rubro : indicadores.getVolumenPorRubro()) {
            rubros.add(rubro.getRubro());
            volumen.add(rubro.getVolumen());
        }
        return grafico("bar", rubros, Json.createArrayBuilder().add(serie("columna.volumen", PRIMARIO, volumen)),
                Json.createObjectBuilder().add("indexAxis", "y"));
    }

    public String getGraficoInfracciones() {
        JsonArrayBuilder tipos = Json.createArrayBuilder();
        JsonArrayBuilder confirmadas = Json.createArrayBuilder();
        JsonArrayBuilder desestimadas = Json.createArrayBuilder();
        JsonArrayBuilder pendientes = Json.createArrayBuilder();
        for (InfraccionesPorTipoDto infracciones : indicadores.getInfracciones()) {
            tipos.add(infracciones.getTipo().getNombre());
            confirmadas.add(infracciones.getConfirmadas());
            desestimadas.add(infracciones.getDesestimadas());
            pendientes.add(infracciones.getPendientes());
        }
        return grafico("bar", tipos, Json.createArrayBuilder()
                        .add(serie("columna.confirmadas", CONFIRMADAS, confirmadas))
                        .add(serie("columna.desestimadas", DESESTIMADAS, desestimadas))
                        .add(serie("columna.pendientes", PENDIENTES, pendientes)),
                Json.createObjectBuilder().add("scales", Json.createObjectBuilder()
                        .add("x", Json.createObjectBuilder().add("stacked", true))
                        .add("y", Json.createObjectBuilder().add("stacked", true).add("beginAtZero", true))));
    }

    private static String grafico(String tipo, JsonArrayBuilder etiquetas, JsonArrayBuilder series, JsonObjectBuilder opciones) {
        return Json.createObjectBuilder()
                .add("type", tipo)
                .add("data", Json.createObjectBuilder().add("labels", etiquetas).add("datasets", series))
                .add("options", opciones.add("maintainAspectRatio", false))
                .build()
                .toString();
    }

    private static JsonObjectBuilder serie(String clave, String color, JsonArrayBuilder datos) {
        return Json.createObjectBuilder()
                .add("label", TextosInforme.resolver(clave))
                .add("data", datos)
                .add("backgroundColor", color)
                .add("borderColor", color);
    }

    public StreamedContent archivo(InformeDto informe) {
        byte[] contenido = informeEJB.descargar(informe.getId());
        return DefaultStreamedContent.builder()
                .name("reporte_" + informe.getPeriodoDesde() + "_" + informe.getPeriodoHasta()
                        + "." + informe.getFormato().name().toLowerCase())
                .contentType(informe.getFormato() == FormatoInforme.PDF ? "application/pdf" : "text/csv; charset=UTF-8")
                .stream(() -> new ByteArrayInputStream(contenido))
                .build();
    }

    public String severidad(InformeDto informe) {
        return switch (informe.getEstado()) {
            case PENDIENTE -> "info";
            case GENERADO -> "success";
            case FALLIDO -> "danger";
            case VENCIDO -> "secondary";
        };
    }

    public boolean isTienePendientes() {
        return informeEJB.tienePendientes();
    }

    public ModeloPaginado<InformeDto> getInformes() {
        return informes;
    }

    public List<TipoCargaDto> getTiposCarga() {
        if (tiposCarga == null) {
            tiposCarga = guiaEJB.getTiposCarga();
        }
        return tiposCarga;
    }

    public IndicadoresDto getIndicadores() {
        return indicadores;
    }

    public LocalDate getDesde() {
        return desde;
    }

    public void setDesde(LocalDate desde) {
        this.desde = desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    public void setHasta(LocalDate hasta) {
        this.hasta = hasta;
    }

    public Long getIdTipoCarga() {
        return idTipoCarga;
    }

    public void setIdTipoCarga(Long idTipoCarga) {
        this.idTipoCarga = idTipoCarga;
    }

    private static void mensaje(String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, texto, null));
    }
}
