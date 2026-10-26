package tse.cargauy.negocio.informe;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import jakarta.annotation.security.PermitAll;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import tse.cargauy.data.informe.InformeDAOLocal;
import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.IndicadoresDto;
import tse.cargauy.dtos.InformeDto;
import tse.cargauy.dtos.InfraccionesPorTipoDto;
import tse.cargauy.dtos.ViajesPorPeriodoDto;
import tse.cargauy.dtos.VolumenPorRubroDto;
import tse.cargauy.entities.EstadoInforme;
import tse.cargauy.entities.FormatoInforme;

@Stateless
@PermitAll
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class GeneradorInformesEJB implements GeneradorInformesEJBLocal {

    static final int DIAS_RETENCION = 30;

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale URUGUAY = Locale.of("es", "UY");

    private static final Logger LOGGER = Logger.getLogger(GeneradorInformesEJB.class.getName());

    @EJB
    InformeDAOLocal informeDAO;

    public void generar(Long idInforme) {
        InformeDto informe = informeDAO.getInformeById(idInforme);
        if (informe == null || informe.getEstado() != EstadoInforme.PENDIENTE) {
            return;
        }
        LocalDateTime ahora = ahora();
        try {
            FiltroInforme filtro = new FiltroInforme(informe.getPeriodoDesde(), informe.getPeriodoHasta(), informe.getIdTipoCarga());
            IndicadoresDto indicadores = InformeEJB.calcular(informeDAO, filtro, ahora.toLocalDate());
            byte[] contenido = exportar(informe, secciones(indicadores), ahora);
            informeDAO.guardarContenido(idInforme, contenido, ahora());
            LOGGER.info("informe generacion=GENERADO informe=" + idInforme + " bytes=" + contenido.length);
        } catch (RuntimeException e) {
            informeDAO.marcarFallido(idInforme);
            LOGGER.warning("informe generacion=FALLIDA informe=" + idInforme + " error=" + e);
        }
    }

    @Schedule(hour = "4", minute = "30", persistent = false)
    public void vencerViejos() {
        int vencidos = informeDAO.vencer(ahora().minusDays(DIAS_RETENCION));
        if (vencidos > 0) {
            LOGGER.info("informe vencimiento vencidos=" + vencidos);
        }
    }

    LocalDateTime ahora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private static byte[] exportar(InformeDto informe, List<Seccion> secciones, LocalDateTime ahora) {
        if (informe.getFormato() == FormatoInforme.CSV) {
            return ExportadorCsv.exportar(secciones);
        }
        String tipoCarga = informe.getTipoCarga() == null ? TextosInforme.resolver("informe.tipoCarga.todos") : informe.getTipoCarga();
        return ExportadorPdf.exportar(TextosInforme.resolver("informe.titulo"), List.of(
                TextosInforme.resolver("informe.periodo", TextosInforme.resolver("informe.rango",
                        informe.getPeriodoDesde().format(DIA), informe.getPeriodoHasta().format(DIA))),
                TextosInforme.resolver("informe.tipoCarga", tipoCarga),
                TextosInforme.resolver("informe.generado", ahora.format(HORA))), secciones);
    }

    static List<Seccion> secciones(IndicadoresDto indicadores) {
        String periodo = TextosInforme.resolver("informe.rango", indicadores.getFiltro().getDesde().format(DIA),
                indicadores.getFiltro().getHasta().format(DIA));
        return List.of(
                new Seccion("seccion.habilitados", new Tabla<>(List.of(indicadores), List.of(
                        new Columna<IndicadoresDto>("columna.fecha", i -> i.getHabilitadosAl().format(DIA)),
                        new Columna<IndicadoresDto>("columna.empresasHabilitadas", IndicadoresDto::getEmpresasHabilitadas),
                        new Columna<IndicadoresDto>("columna.vehiculosHabilitados", IndicadoresDto::getVehiculosHabilitados)))),
                new Seccion("seccion.tasa", new Tabla<>(List.of(indicadores), List.of(
                        new Columna<IndicadoresDto>("columna.periodo", i -> periodo),
                        new Columna<IndicadoresDto>("columna.detectadas", IndicadoresDto::getDetectadas),
                        new Columna<IndicadoresDto>("columna.confirmadas", IndicadoresDto::getConfirmadas),
                        new Columna<IndicadoresDto>("columna.tasa", i -> i.getTasaConfirmadas() == null ? null
                                : String.format(URUGUAY, "%.1f %%", i.getTasaConfirmadas() * 100))))),
                new Seccion("seccion.viajes", new Tabla<>(indicadores.getViajes(), List.of(
                        new Columna<ViajesPorPeriodoDto>("columna.periodo", indicadores::periodo),
                        new Columna<ViajesPorPeriodoDto>("columna.viajes", ViajesPorPeriodoDto::getCantidad)))),
                new Seccion("seccion.volumen", new Tabla<>(indicadores.getVolumenPorRubro(), List.of(
                        new Columna<VolumenPorRubroDto>("columna.rubro", VolumenPorRubroDto::getRubro),
                        new Columna<VolumenPorRubroDto>("columna.volumen", VolumenPorRubroDto::getVolumen)))),
                new Seccion("seccion.infracciones", new Tabla<>(indicadores.getInfracciones(), List.of(
                        new Columna<InfraccionesPorTipoDto>("columna.tipo", i -> i.getTipo().getNombre()),
                        new Columna<InfraccionesPorTipoDto>("columna.detectadas", InfraccionesPorTipoDto::getDetectadas),
                        new Columna<InfraccionesPorTipoDto>("columna.confirmadas", InfraccionesPorTipoDto::getConfirmadas),
                        new Columna<InfraccionesPorTipoDto>("columna.desestimadas", InfraccionesPorTipoDto::getDesestimadas),
                        new Columna<InfraccionesPorTipoDto>("columna.pendientes", InfraccionesPorTipoDto::getPendientes)))));
    }
}
