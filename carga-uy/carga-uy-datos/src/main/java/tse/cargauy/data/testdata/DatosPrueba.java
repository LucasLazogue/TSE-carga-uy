package tse.cargauy.data.testdata;

import java.time.LocalDate;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import tse.cargauy.data.empresa.EmpresaDAOLocal;
import tse.cargauy.data.guia.GuiaDAOLocal;
import tse.cargauy.data.permiso.PermisoDAOLocal;
import tse.cargauy.data.usuario.UsuarioDAOLocal;
import tse.cargauy.data.vehiculo.VehiculoDAOLocal;
import tse.cargauy.data.viaje.ViajeDAOLocal;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.Rol;

@Singleton
@Startup
public class DatosPrueba {

    @EJB
    private EmpresaDAOLocal empresaDAO;

    @EJB
    private VehiculoDAOLocal vehiculoDAO;

    @EJB
    private PermisoDAOLocal permisoDAO;

    @EJB
    private UsuarioDAOLocal usuarioDAO;

    @EJB
    private GuiaDAOLocal guiaDAO;

    @EJB
    private ViajeDAOLocal viajeDAO;

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

        if (permisoDAO.getAll().isEmpty() && vehiculoDAO.getAll().size() >= 4) {
            Long sta = vehiculoDAO.getVehiculoByMatricula("STA1234").getId();
            Long sbb = vehiculoDAO.getVehiculoByMatricula("SBB5678").getId();
            Long scc = vehiculoDAO.getVehiculoByMatricula("SCC9012").getId();
            Long sdd = vehiculoDAO.getVehiculoByMatricula("SDD3456").getId();

            permisoDAO.addPermiso(new PermisoDto("PNC-1001", LocalDate.of(2024, 1, 1), LocalDate.of(2025, 12, 31), sta));
            permisoDAO.addPermiso(new PermisoDto("PNC-1002", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), sta));
            permisoDAO.addPermiso(new PermisoDto("PNC-1003", LocalDate.of(2026, 3, 1), LocalDate.of(2027, 2, 28), sbb));
            permisoDAO.addPermiso(new PermisoDto("PNC-1004", LocalDate.of(2026, 2, 1), LocalDate.of(2027, 1, 31), scc));
            permisoDAO.addPermiso(new PermisoDto("PNC-1005", LocalDate.of(2025, 6, 1), LocalDate.of(2026, 5, 31), sdd));
        }

        if (usuarioDAO.getRoles().isEmpty()) {
            usuarioDAO.addRol(Rol.CIUDADANO);
            usuarioDAO.addRol(Rol.FUNCIONARIO);
            usuarioDAO.addFuncionario("11111111");
        }

        UsuarioDto carlos = usuarioDAO.getCiudadanoByCedula("55555555");
        if (carlos == null) {
            carlos = usuarioDAO.addCiudadano("55555555", "carlos.suarez@example.com");
        }
        LocalDate ingreso = LocalDate.of(2024, 1, 1);
        if (usuarioDAO.getChoferVigente(carlos.getId(), empresas.get(0).getId(), ingreso) == null) {
            usuarioDAO.addChofer(carlos.getId(), empresas.get(0).getId(), ingreso);
        }
        if (usuarioDAO.getResponsableVigente(carlos.getId(), empresas.get(0).getId(), ingreso) == null) {
            usuarioDAO.addResponsable(carlos.getId(), empresas.get(0).getId(), ingreso);
        }

        if (guiaDAO.getRubros().isEmpty()) {
            for (String rubro : List.of("Agropecuario", "Comercio", "Construccion", "Forestal", "Industria", "Logistica")) {
                guiaDAO.addRubro(rubro);
            }
            for (String tipo : List.of("Carga general", "Granos", "Madera", "Contenedores", "Refrigerada", "Combustibles")) {
                guiaDAO.addTipoCarga(tipo);
            }
        }

        if (guiaDAO.getAll().isEmpty() && empresas.size() >= 3) {
            List<RubroDto> rubros = guiaDAO.getRubros();
            List<TipoCargaDto> tipos = guiaDAO.getTiposCarga();
            Long chofer = carlos.getId();
            Long sta = vehiculoDAO.getVehiculoByMatricula("STA1234").getId();

            GuiaDto montevideoRivera = guiaDAO.addGuia(new GuiaDto(LocalDate.of(2026, 10, 15),
                    -34.9011, -56.1645, -30.9053, -55.5508, 18000,
                    empresas.get(0).getId(), chofer, rubros.get(0).getId(), tipos.get(3).getId()));
            viajeDAO.addViaje(new ViajeDto(montevideoRivera.getId(), sta, chofer));

            guiaDAO.addGuia(new GuiaDto(LocalDate.of(2026, 10, 20),
                    -33.1325, -58.2956, -34.9058, -56.2132, 20000,
                    empresas.get(0).getId(), chofer, rubros.get(3).getId(), tipos.get(4).getId()));
        }
    }
}
