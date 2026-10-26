package tse.cargauy.web.beans;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.IndicadoresDto;
import tse.cargauy.dtos.InformeDto;
import tse.cargauy.dtos.TipoCargaDto;
import tse.cargauy.entities.FormatoInforme;
import tse.cargauy.entities.TipoInforme;
import tse.cargauy.negocio.guia.GuiaEJBLocal;
import tse.cargauy.negocio.informe.InformeEJBLocal;

@Named
@RequestScoped
public class InformeView {

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

    public void exportar(TipoInforme tipo, FormatoInforme formato) {
        informeEJB.exportar(new FiltroInforme(desde, hasta, idTipoCarga), tipo, formato);
        mensaje("Se está generando el reporte. Lo vas a encontrar en Mis exportaciones.");
    }

    public StreamedContent archivo(InformeDto informe) {
        byte[] contenido = informeEJB.descargar(informe.getId());
        return DefaultStreamedContent.builder()
                .name(informe.getTipo().name().toLowerCase() + "_" + informe.getPeriodoDesde() + "_" + informe.getPeriodoHasta()
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
