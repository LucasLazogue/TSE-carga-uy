package tse.cargauy.negocio.guia;

import java.util.List;
import jakarta.ejb.Local;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;

@Local
public interface GuiaEJBLocal {
    GuiaDto getGuiaById(Long id);
    List<GuiaDto> getAll();
    List<GuiaDto> getByEmpresa(Long idEmpresa);
    GuiaDto addGuia(GuiaDto guiaDto);
    void updateGuia(Long id, GuiaDto guiaDto);
    void deleteGuia(Long id);
    List<RubroDto> getRubros();
    List<TipoCargaDto> getTiposCarga();
}
