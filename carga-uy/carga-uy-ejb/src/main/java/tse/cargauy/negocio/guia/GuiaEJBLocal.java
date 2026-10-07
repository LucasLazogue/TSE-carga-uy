package tse.cargauy.negocio.guia;

import java.util.List;

import jakarta.ejb.Local;
import tse.cargauy.dtos.FiltroGuias;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;

@Local
public interface GuiaEJBLocal {

    PaginaDto<GuiaDto> listar(Long idEmpresa, FiltroGuias filtro, Paginacion paginacion);

    GuiaDto getGuia(Long idEmpresa, Long id);

    GuiaDto addGuia(Long idEmpresa, GuiaDto guiaDto);

    GuiaDto updateGuia(Long idEmpresa, Long id, GuiaDto guiaDto);

    void deleteGuia(Long idEmpresa, Long id);

    List<RubroDto> getRubros();

    List<TipoCargaDto> getTiposCarga();
}
