package tse.cargauy.data.testdata;

import java.time.LocalDate;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import tse.cargauy.data.empresa.EmpresaDAOLocal;
import tse.cargauy.data.vehiculo.VehiculoDAOLocal;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.VehiculoDto;

@Singleton
@Startup
public class DatosPrueba {

    @EJB
    private EmpresaDAOLocal empresaDAO;

    @EJB
    private VehiculoDAOLocal vehiculoDAO;

    @PostConstruct
    public void init() {
        // solo la primera vez, si no se duplican en cada arranque
        if (empresaDAO.getAll().isEmpty()) {
            empresaDAO.addEmpresa(new EmpresaDto(1001, "Transportes del Sur", "Transportes del Sur S.A.", "Av. Italia 3456, Montevideo", LocalDate.of(2020, 3, 15)));
            empresaDAO.addEmpresa(new EmpresaDto(1002, "Cargas Norte", "Cargas del Norte S.R.L.", "Ruta 5 Km 480, Rivera", LocalDate.of(2019, 7, 1)));
            empresaDAO.addEmpresa(new EmpresaDto(1003, "LogiEste", "Logistica del Este Ltda.", "Av. Roosevelt 1200, Maldonado", LocalDate.of(2021, 11, 20)));
        }

        List<EmpresaDto> empresas = empresaDAO.getAll();
        if (vehiculoDAO.getAll().isEmpty() && empresas.size() >= 3) {
            vehiculoDAO.addVehiculo(new VehiculoDto("STA1234", "Volvo", "FH 460", 8500, 24000, empresas.get(0).getId()));
            vehiculoDAO.addVehiculo(new VehiculoDto("SBB5678", "Scania", "R 450", 9000, 26000, empresas.get(0).getId()));
            vehiculoDAO.addVehiculo(new VehiculoDto("SCC9012", "Mercedes-Benz", "Actros 2646", 8800, 25000, empresas.get(1).getId()));
            vehiculoDAO.addVehiculo(new VehiculoDto("SDD3456", "Iveco", "Stralis 480", 8200, 23000, empresas.get(2).getId()));
        }
    }
}
