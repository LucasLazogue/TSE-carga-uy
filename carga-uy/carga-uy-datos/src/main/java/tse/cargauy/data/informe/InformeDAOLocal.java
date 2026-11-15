package tse.cargauy.data.informe;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.ejb.Local;
import tse.cargauy.dtos.CasoHistoricoDto;
import tse.cargauy.dtos.InformeDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.ViajeHistoricoDto;

@Local
public interface InformeDAOLocal {
    long contarVehiculosHabilitados(LocalDate fecha);
    long contarEmpresasHabilitadas(LocalDate fecha);
    boolean isEmpresaHabilitada(Long idEmpresa, LocalDate fecha);
    ViajeHistoricoDto getViajeHistorico(Long idViaje);
    CasoHistoricoDto getCasoHistorico(Long idCaso);
    InformeDto addInforme(InformeDto informeDto);
    PaginaDto<InformeDto> getBySolicitante(Long idSolicitante, Paginacion paginacion);
    InformeDto getInformeById(Long id);
    boolean tienePendientes(Long idSolicitante);
    void guardarContenido(Long id, byte[] contenido, LocalDateTime fecha);
    void marcarFallido(Long id);
    int vencer(LocalDateTime generadosAntesDe);
    byte[] getContenido(Long id);
}
