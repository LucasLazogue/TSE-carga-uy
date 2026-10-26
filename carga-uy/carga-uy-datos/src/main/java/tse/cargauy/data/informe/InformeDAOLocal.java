package tse.cargauy.data.informe;

import java.time.LocalDate;
import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.dtos.ConteoDto;
import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.InfraccionesPorTipoDto;
import tse.cargauy.dtos.ViajesPorDiaDto;

@Local
public interface InformeDAOLocal {
    long contarVehiculosHabilitados(LocalDate fecha);
    long contarEmpresasHabilitadas(LocalDate fecha);
    List<ViajesPorDiaDto> contarViajesPorDia(FiltroInforme filtro);
    List<ConteoDto> sumarVolumenPorRubro(FiltroInforme filtro);
    List<InfraccionesPorTipoDto> contarInfracciones(FiltroInforme filtro);
}
