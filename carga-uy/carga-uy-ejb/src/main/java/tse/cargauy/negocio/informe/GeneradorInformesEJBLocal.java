package tse.cargauy.negocio.informe;

import jakarta.ejb.Local;

@Local
public interface GeneradorInformesEJBLocal {
    void generar(Long idInforme);
}
