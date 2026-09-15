package tse.cargauy.data.testdata;

import java.time.LocalDate;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import tse.cargauy.data.empresa.EmpresaDAOLocal;
import tse.cargauy.dtos.EmpresaDto;

@Singleton
@Startup
public class DatosPrueba {

    @EJB
    private EmpresaDAOLocal empresaDAO;

    @PostConstruct
    public void init() {
        // solo la primera vez, si no se duplican en cada arranque
        if (empresaDAO.getAll().isEmpty()) {
            empresaDAO.addEmpresa(new EmpresaDto(1001, "Transportes del Sur", "Transportes del Sur S.A.", "Av. Italia 3456, Montevideo", LocalDate.of(2020, 3, 15)));
            empresaDAO.addEmpresa(new EmpresaDto(1002, "Cargas Norte", "Cargas del Norte S.R.L.", "Ruta 5 Km 480, Rivera", LocalDate.of(2019, 7, 1)));
            empresaDAO.addEmpresa(new EmpresaDto(1003, "LogiEste", "Logistica del Este Ltda.", "Av. Roosevelt 1200, Maldonado", LocalDate.of(2021, 11, 20)));
        }
    }
}
