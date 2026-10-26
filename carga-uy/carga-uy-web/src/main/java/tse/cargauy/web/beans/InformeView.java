package tse.cargauy.web.beans;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.IndicadoresDto;
import tse.cargauy.dtos.TipoCargaDto;
import tse.cargauy.dtos.ViajesPorPeriodoDto;
import tse.cargauy.negocio.guia.GuiaEJBLocal;
import tse.cargauy.negocio.informe.InformeEJBLocal;

@Named
@RequestScoped
public class InformeView {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MM/yyyy");

    @EJB
    private InformeEJBLocal informeEJB;

    @EJB
    private GuiaEJBLocal guiaEJB;

    private LocalDate desde;
    private LocalDate hasta;
    private Long idTipoCarga;
    private IndicadoresDto indicadores;
    private List<TipoCargaDto> tiposCarga;

    @PostConstruct
    public void init() {
        hasta = LocalDate.now();
        desde = hasta.withDayOfYear(1);
    }

    public void consultar() {
        indicadores = informeEJB.consultar(new FiltroInforme(desde, hasta, idTipoCarga));
    }

    public String periodo(ViajesPorPeriodoDto viajes) {
        return switch (indicadores.getGranularidad()) {
            case DIA -> viajes.getInicio().format(DIA);
            case SEMANA -> "Semana del " + viajes.getInicio().format(DIA);
            case MES -> viajes.getInicio().format(MES);
        };
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
}
