package tse.cargauy.data.testdata;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import tse.cargauy.data.empresa.EmpresaDAOLocal;
import tse.cargauy.data.guia.GuiaDAOLocal;
import tse.cargauy.data.itv.ItvDAOLocal;
import tse.cargauy.data.permiso.PermisoDAOLocal;
import tse.cargauy.data.usuario.UsuarioDAOLocal;
import tse.cargauy.data.vehiculo.VehiculoDAOLocal;
import tse.cargauy.data.viaje.ViajeDAOLocal;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.ItvDto;
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoPermiso;
import tse.cargauy.entities.EstadoViaje;

// la informacion publica (frontoffice, Estadisticas) solo muestra un mes o un rubro si detras hay al menos 5 empresas
// y 5 vehiculos (InformeDAO). Con los datos de prueba solo Transportes del Sur tenia viajes y la pantalla quedaba
// vacia. Esto agrega cinco empresas mas, cada una con un camion y un viaje terminado por mes de febrero a octubre,
// rotando el rubro: asi cada mes y cada rubro supera el umbral. Las personas son ficticias (cedulas 3000000x)
final class HistoriaPublica {

    static final int PRIMER_MES = 2;
    static final int ULTIMO_MES = 10;
    static final int ANIO = 2026;
    private static final LocalDate ALTA = LocalDate.of(2024, 1, 1);

    record EmpresaPublica(int nro, String nombre, String razonSocial, String direccion, String matricula, String marca,
            String modelo, int peso, int capacidad, String cedula) {
    }

    static final List<EmpresaPublica> EMPRESAS = List.of(
            new EmpresaPublica(1005, "Rutas del Este", "Rutas del Este S.A.", "Ruta 8 Km 285, Treinta y Tres",
                    "SFA2101", "Volvo", "FM 420", 8300, 23500, "30000001"),
            new EmpresaPublica(1006, "Transportes Colonia", "Transportes Colonia S.R.L.", "Av. Artigas 450, Colonia",
                    "SFB2102", "Scania", "G 410", 8600, 24500, "30000002"),
            new EmpresaPublica(1007, "Fletes del Centro", "Fletes del Centro Ltda.", "Ruta 5 Km 389, Tacuarembo",
                    "SFC2103", "Mercedes-Benz", "Axor 2041", 8100, 22000, "30000003"),
            new EmpresaPublica(1008, "Logistica Oeste", "Logistica Oeste S.A.", "Av. Espana 1020, Paysandu",
                    "SFD2104", "Iveco", "S-Way 460", 8400, 24000, "30000004"),
            new EmpresaPublica(1009, "Cargas Florida", "Cargas Florida S.R.L.", "Ruta 5 Km 98, Florida",
                    "SFE2105", "DAF", "CF 450", 8200, 23000, "30000005"));

    // un viaje terminado: de que empresa (indice en EMPRESAS), en que mes y con que rubro y tipo de carga (indices)
    record ViajePublico(int empresa, int mes, int rubro, int tipoCarga, int volumen) {
    }

    private final EmpresaDAOLocal empresaDAO;
    private final VehiculoDAOLocal vehiculoDAO;
    private final PermisoDAOLocal permisoDAO;
    private final ItvDAOLocal itvDAO;
    private final UsuarioDAOLocal usuarioDAO;
    private final GuiaDAOLocal guiaDAO;
    private final ViajeDAOLocal viajeDAO;

    HistoriaPublica(EmpresaDAOLocal empresaDAO, VehiculoDAOLocal vehiculoDAO, PermisoDAOLocal permisoDAO, ItvDAOLocal itvDAO,
            UsuarioDAOLocal usuarioDAO, GuiaDAOLocal guiaDAO, ViajeDAOLocal viajeDAO) {
        this.empresaDAO = empresaDAO;
        this.vehiculoDAO = vehiculoDAO;
        this.permisoDAO = permisoDAO;
        this.itvDAO = itvDAO;
        this.usuarioDAO = usuarioDAO;
        this.guiaDAO = guiaDAO;
        this.viajeDAO = viajeDAO;
    }

    // solo la primera vez: si la primera de estas empresas ya esta, se cargo todo junto
    void cargar() {
        if (empresaDAO.getEmpresaByNro(EMPRESAS.get(0).nro()) != null) {
            return;
        }
        List<RubroDto> rubros = guiaDAO.getRubros();
        List<TipoCargaDto> tipos = guiaDAO.getTiposCarga();
        List<Long> empresas = new ArrayList<>();
        List<Long> vehiculos = new ArrayList<>();
        List<Long> personas = new ArrayList<>();
        for (EmpresaPublica datos : EMPRESAS) {
            EmpresaDto empresa = empresaDAO.addEmpresa(new EmpresaDto(datos.nro(), datos.nombre(), datos.razonSocial(),
                    datos.direccion(), ALTA));
            vehiculoDAO.addVehiculo(new VehiculoDto(datos.matricula(), datos.marca(), datos.modelo(), datos.peso(),
                    datos.capacidad(), empresa.getId()));
            Long vehiculo = vehiculoDAO.getVehiculoByMatricula(datos.matricula()).getId();
            permisoDAO.addPermiso(new PermisoDto("PNC-" + (datos.nro() + 1000), LocalDate.of(ANIO, 1, 1), LocalDate.of(ANIO, 12, 31),
                    vehiculo, EstadoPermiso.APROBADO));
            itvDAO.addItv(new ItvDto(vehiculo, LocalDate.of(ANIO, 1, 10), LocalDate.of(ANIO + 1, 1, 10)));
            // la misma persona es responsable (registra la guia) y chofer, como Carlos en Transportes del Sur
            UsuarioDto persona = usuarioDAO.addCiudadano(datos.cedula(), "chofer" + datos.nro() + "@example.com");
            usuarioDAO.addResponsable(persona.getId(), empresa.getId(), ALTA);
            usuarioDAO.addChofer(persona.getId(), empresa.getId(), ALTA);
            empresas.add(empresa.getId());
            vehiculos.add(vehiculo);
            personas.add(persona.getId());
        }
        for (ViajePublico viaje : viajes(EMPRESAS.size(), rubros.size(), tipos.size())) {
            LocalDate fecha = LocalDate.of(ANIO, viaje.mes(), 10 + viaje.empresa());
            GuiaDto guia = guiaDAO.addGuia(new GuiaDto(fecha, -34.9011, -56.1645, -31.3833, -57.9667, viaje.volumen(),
                    empresas.get(viaje.empresa()), personas.get(viaje.empresa()), rubros.get(viaje.rubro()).getId(),
                    tipos.get(viaje.tipoCarga()).getId()));
            Long id = viajeDAO.addViaje(new ViajeDto(guia.getId(), vehiculos.get(viaje.empresa()), personas.get(viaje.empresa())))
                    .getId();
            viajeDAO.cambiarEstado(id, EstadoViaje.EN_CURSO, fecha.atTime(7, 0));
            viajeDAO.cambiarEstado(id, EstadoViaje.FINALIZADO, fecha.atTime(17, 0));
        }
    }

    // cada empresa viaja todos los meses y el rubro avanza uno por mes, corrido segun la empresa: como son mas meses
    // que rubros, cada empresa pasa por todos los rubros y cada rubro queda con las cinco empresas
    static List<ViajePublico> viajes(int empresas, int rubros, int tipos) {
        List<ViajePublico> viajes = new ArrayList<>();
        for (int mes = PRIMER_MES; mes <= ULTIMO_MES; mes++) {
            for (int empresa = 0; empresa < empresas; empresa++) {
                viajes.add(new ViajePublico(empresa, mes, (mes + empresa) % rubros, (mes + 2 * empresa) % tipos,
                        14000 + 600 * empresa + 400 * mes));
            }
        }
        return viajes;
    }
}
