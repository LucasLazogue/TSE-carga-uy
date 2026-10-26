package tse.cargauy.negocio.informe;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import tse.cargauy.data.informe.InformeDAOLocal;
import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.Granularidad;
import tse.cargauy.dtos.IndicadoresDto;
import tse.cargauy.dtos.InfraccionesPorTipoDto;
import tse.cargauy.dtos.ViajesPorPeriodoDto;
import tse.cargauy.entities.Rol;
import tse.cargauy.entities.TipoInfraccion;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

@Stateless
@RolesAllowed(Rol.AUTORIDAD)
public class InformeEJB implements InformeEJBLocal {

    static final int MESES_PERIODO_AMPLIO = 12;
    static final int ANIOS_PERIODO_MAXIMO = 5;
    static final int MESES_POR_DIA = 2;
    static final int MESES_POR_SEMANA = 6;

    @EJB
    InformeDAOLocal informeDAO;

    public IndicadoresDto consultar(FiltroInforme filtro) {
        validar(filtro);
        LocalDate hoy = hoy();
        LocalDate habilitadosAl = filtro.getHasta().isAfter(hoy) ? hoy : filtro.getHasta();
        Granularidad granularidad = granularidad(filtro);

        IndicadoresDto indicadores = new IndicadoresDto();
        indicadores.setFiltro(filtro);
        indicadores.setPeriodoAmplio(filtro.getHasta().isAfter(filtro.getDesde().plusMonths(MESES_PERIODO_AMPLIO)));
        indicadores.setHabilitadosAl(habilitadosAl);
        indicadores.setEmpresasHabilitadas(informeDAO.contarEmpresasHabilitadas(habilitadosAl));
        indicadores.setVehiculosHabilitados(informeDAO.contarVehiculosHabilitados(habilitadosAl));
        indicadores.setGranularidad(granularidad);
        indicadores.setViajes(agrupar(informeDAO.contarViajesPorDia(filtro), granularidad));
        indicadores.setVolumenPorRubro(informeDAO.sumarVolumenPorRubro(filtro));
        indicadores.setInfracciones(porTipo(informeDAO.contarInfracciones(filtro)));
        return indicadores;
    }

    LocalDate hoy() {
        return LocalDate.now();
    }

    private static void validar(FiltroInforme filtro) {
        if (filtro.getDesde() == null || filtro.getHasta() == null) {
            throw new CargaUYException(CodigoError.INFORME_PERIODO_REQUERIDO);
        }
        if (filtro.getHasta().isBefore(filtro.getDesde())) {
            throw new CargaUYException(CodigoError.INFORME_RANGO_FECHAS_INVALIDO);
        }
        if (filtro.getHasta().isAfter(filtro.getDesde().plusYears(ANIOS_PERIODO_MAXIMO))) {
            throw new CargaUYException(CodigoError.INFORME_PERIODO_DEMASIADO_LARGO, String.valueOf(ANIOS_PERIODO_MAXIMO));
        }
    }

    private static Granularidad granularidad(FiltroInforme filtro) {
        if (!filtro.getHasta().isAfter(filtro.getDesde().plusMonths(MESES_POR_DIA))) {
            return Granularidad.DIA;
        }
        if (!filtro.getHasta().isAfter(filtro.getDesde().plusMonths(MESES_POR_SEMANA))) {
            return Granularidad.SEMANA;
        }
        return Granularidad.MES;
    }

    private static List<ViajesPorPeriodoDto> agrupar(List<ViajesPorPeriodoDto> porDia, Granularidad granularidad) {
        if (granularidad == Granularidad.DIA) {
            return porDia;
        }
        Map<LocalDate, Long> porPeriodo = new TreeMap<>();
        for (ViajesPorPeriodoDto dia : porDia) {
            LocalDate inicio = granularidad == Granularidad.SEMANA
                    ? dia.getInicio().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    : dia.getInicio().withDayOfMonth(1);
            porPeriodo.merge(inicio, dia.getCantidad(), Long::sum);
        }
        List<ViajesPorPeriodoDto> viajes = new ArrayList<>();
        porPeriodo.forEach((inicio, cantidad) -> viajes.add(new ViajesPorPeriodoDto(inicio, cantidad)));
        return viajes;
    }

    private static List<InfraccionesPorTipoDto> porTipo(List<InfraccionesPorTipoDto> contadas) {
        Map<TipoInfraccion, InfraccionesPorTipoDto> porTipo = new EnumMap<>(TipoInfraccion.class);
        for (TipoInfraccion tipo : TipoInfraccion.values()) {
            porTipo.put(tipo, new InfraccionesPorTipoDto(tipo, 0, 0, 0));
        }
        contadas.forEach(i -> porTipo.put(i.getTipo(), i));
        return new ArrayList<>(porTipo.values());
    }
}
