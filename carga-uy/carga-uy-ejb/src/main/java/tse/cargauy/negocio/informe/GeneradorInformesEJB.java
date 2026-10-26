package tse.cargauy.negocio.informe;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import jakarta.annotation.security.PermitAll;
import jakarta.ejb.EJB;
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
import tse.cargauy.entities.TipoInforme;

@Stateless
@PermitAll
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class GeneradorInformesEJB implements GeneradorInformesEJBLocal {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
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
            byte[] contenido = exportar(informe.getFormato(), filas(informe.getTipo(), indicadores));
            informeDAO.guardarContenido(idInforme, contenido, ahora());
            LOGGER.info("informe generacion=GENERADO informe=" + idInforme + " bytes=" + contenido.length);
        } catch (RuntimeException e) {
            informeDAO.marcarFallido(idInforme);
            LOGGER.warning("informe generacion=FALLIDA informe=" + idInforme + " error=" + e);
        }
    }

    LocalDateTime ahora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private static byte[] exportar(FormatoInforme formato, List<String[]> filas) {
        if (formato == FormatoInforme.CSV) {
            return ExportadorCsv.exportar(filas);
        }
        throw new UnsupportedOperationException("formato " + formato);
    }

    static List<String[]> filas(TipoInforme tipo, IndicadoresDto indicadores) {
        List<String[]> filas = new ArrayList<>();
        switch (tipo) {
            case HABILITADOS -> {
                filas.add(new String[] { "Fecha", "Empresas habilitadas", "Vehículos habilitados" });
                filas.add(new String[] { indicadores.getHabilitadosAl().format(DIA),
                        String.valueOf(indicadores.getEmpresasHabilitadas()), String.valueOf(indicadores.getVehiculosHabilitados()) });
            }
            case VIAJES_POR_PERIODO -> {
                filas.add(new String[] { "Período", "Viajes" });
                for (ViajesPorPeriodoDto viajes : indicadores.getViajes()) {
                    filas.add(new String[] { indicadores.periodo(viajes), String.valueOf(viajes.getCantidad()) });
                }
            }
            case VOLUMEN_POR_RUBRO -> {
                filas.add(new String[] { "Rubro", "Volumen (kg)" });
                for (VolumenPorRubroDto volumen : indicadores.getVolumenPorRubro()) {
                    filas.add(new String[] { volumen.getRubro(), String.valueOf(volumen.getVolumen()) });
                }
            }
            case INFRACCIONES_POR_TIPO -> {
                filas.add(new String[] { "Tipo", "Detectadas", "Confirmadas", "Desestimadas", "Pendientes" });
                for (InfraccionesPorTipoDto infracciones : indicadores.getInfracciones()) {
                    filas.add(new String[] { infracciones.getTipo().getNombre(), String.valueOf(infracciones.getDetectadas()),
                            String.valueOf(infracciones.getConfirmadas()), String.valueOf(infracciones.getDesestimadas()),
                            String.valueOf(infracciones.getPendientes()) });
                }
            }
            case TASA_CONFIRMACION -> {
                Double tasa = indicadores.getTasaConfirmadas();
                filas.add(new String[] { "Detectadas", "Confirmadas", "Tasa" });
                filas.add(new String[] { String.valueOf(indicadores.getDetectadas()), String.valueOf(indicadores.getConfirmadas()),
                        tasa == null ? "" : String.format(URUGUAY, "%.1f %%", tasa * 100) });
            }
        }
        return filas;
    }
}
