package tse.cargauy.data.fiscalizacion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.ejb.Local;
import tse.cargauy.dtos.CasoFiscalizacionDto;
import tse.cargauy.dtos.EvidenciaDto;
import tse.cargauy.dtos.FiltroCasos;
import tse.cargauy.dtos.NotificacionDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PistaAuditoriaDto;
import tse.cargauy.entities.EstadoCaso;

@Local
public interface CasoFiscalizacionDAOLocal {
    CasoFiscalizacionDto getCasoById(Long id);
    CasoFiscalizacionDto getByClave(String claveDeduplicacion);
    PaginaDto<CasoFiscalizacionDto> buscar(FiltroCasos filtro, Paginacion paginacion);
    CasoFiscalizacionDto addCaso(CasoFiscalizacionDto casoDto, List<EvidenciaDto> evidencias, LocalDateTime fecha);
    // solo las que el caso todavia no tiene, por su idOrigen
    void agregarEvidencias(Long idCaso, List<EvidenciaDto> evidencias);
    void notificar(Long id, LocalDateTime fecha, LocalDateTime venceDescargo);
    void presentarDescargo(Long id, Long idCiudadano, String texto, LocalDateTime fecha);
    void resolver(Long id, Long idFuncionario, EstadoCaso resultado, String fundamento, LocalDateTime fecha);
    List<PistaAuditoriaDto> getAuditoria(Long idCaso);
    PaginaDto<NotificacionDto> getNotificaciones(List<Long> idsEmpresa, Paginacion paginacion);
    Integer getNormaVigente(String clave, LocalDate fecha);
    void addNorma(String clave, int valor, LocalDate vigenteDesde);
}
