package tse.cargauy.historico;

import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.dtos.CasoHistoricoDto;
import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.InfraccionesPorTipoDto;
import tse.cargauy.dtos.ViajeHistoricoDto;
import tse.cargauy.dtos.ViajesPorMesPublicoDto;
import tse.cargauy.dtos.ViajesPorPeriodoDto;
import tse.cargauy.dtos.VolumenPorRubroDto;
import tse.cargauy.dtos.VolumenPorRubroPublicoDto;

@Local
public interface HistoricoDAOLocal {
    List<ViajesPorPeriodoDto> contarViajesPorDia(FiltroInforme filtro);
    List<VolumenPorRubroDto> sumarVolumenPorRubro(FiltroInforme filtro);
    List<InfraccionesPorTipoDto> contarInfracciones(FiltroInforme filtro);
    List<ViajesPorMesPublicoDto> contarViajesPorMesPublico(FiltroInforme filtro, int umbral);
    List<VolumenPorRubroPublicoDto> sumarVolumenPorRubroPublico(FiltroInforme filtro, int umbral);
    void guardarViaje(ViajeHistoricoDto viaje);
    void guardarCaso(CasoHistoricoDto caso);
    int agregarViajesFaltantes();
    int actualizarCasos();
}
