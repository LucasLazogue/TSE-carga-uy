package tse.cargauy.negocio.publico;

import jakarta.ejb.Local;
import tse.cargauy.dtos.HabilitacionEmpresaDto;

@Local
public interface ConsultaPublicaEJBLocal {
    HabilitacionEmpresaDto validarEmpresa(Integer nroEmpresa, String razonSocial);
}
