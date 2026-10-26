package tse.cargauy.data.informe;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.InformeDto;
import tse.cargauy.dtos.InfraccionesPorTipoDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.ViajesPorPeriodoDto;
import tse.cargauy.dtos.VolumenPorRubroDto;

@Local
public interface InformeDAOLocal {
    long contarVehiculosHabilitados(LocalDate fecha);
    long contarEmpresasHabilitadas(LocalDate fecha);
    List<ViajesPorPeriodoDto> contarViajesPorDia(FiltroInforme filtro);
    List<VolumenPorRubroDto> sumarVolumenPorRubro(FiltroInforme filtro);
    List<InfraccionesPorTipoDto> contarInfracciones(FiltroInforme filtro);
    InformeDto addInforme(InformeDto informeDto);
    PaginaDto<InformeDto> getBySolicitante(Long idSolicitante, Paginacion paginacion);
    InformeDto getInformeById(Long id);
    boolean tienePendientes(Long idSolicitante);
    void guardarContenido(Long id, byte[] contenido, LocalDateTime fecha);
    void marcarFallido(Long id);
    int vencer(LocalDateTime generadosAntesDe);
    byte[] getContenido(Long id);
}
