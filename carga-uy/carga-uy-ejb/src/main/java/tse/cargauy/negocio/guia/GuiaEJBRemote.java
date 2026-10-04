package tse.cargauy.negocio.guia;

import java.util.List;
import jakarta.ejb.Remote;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;

@Remote
public interface GuiaEJBRemote {
    GuiaDto getGuiaById(Long id);
    List<GuiaDto> getAll();
    List<GuiaDto> getByEmpresa(Long idEmpresa);
    GuiaDto addGuia(GuiaDto guiaDto);
    void updateGuia(Long id, GuiaDto guiaDto);
    void deleteGuia(Long id);
    List<RubroDto> getRubros();
    List<TipoCargaDto> getTiposCarga();
}
