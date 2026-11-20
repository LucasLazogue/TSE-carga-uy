package tse.cargauy.data.testdata;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import tse.cargauy.data.empresa.EmpresaDAOLocal;
import tse.cargauy.data.fiscalizacion.CasoFiscalizacionDAOLocal;
import tse.cargauy.data.guia.GuiaDAOLocal;
import tse.cargauy.data.itv.ItvDAOLocal;
import tse.cargauy.data.permiso.PermisoDAOLocal;
import tse.cargauy.data.usuario.UsuarioDAOLocal;
import tse.cargauy.data.vehiculo.VehiculoDAOLocal;
import tse.cargauy.data.viaje.ViajeDAOLocal;
import tse.cargauy.dtos.CasoFiscalizacionDto;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.EvidenciaDto;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.ItvDto;
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.entities.EstadoPermiso;
import tse.cargauy.entities.EstadoViaje;
import tse.cargauy.entities.NormaFiscalizacion;
import tse.cargauy.entities.TipoEvidencia;
import tse.cargauy.entities.TipoInfraccion;

// los reportes de las autoridades (backoffice) cuentan viajes por dia, volumen por rubro e infracciones por tipo. Con los
// datos de la demo habia unos pocos viajes por mes y los casos no tenian viaje, asi que al filtrar por tipo de carga no
// quedaba ninguna infraccion. Esto agrega a las empresas de HistoriaPublica dos camiones mas cada una, viajes terminados
// casi todos los dias habiles de enero a octubre (mas en la zafra, de marzo a mayo) y casos ya resueltos asociados a
// esos viajes. Nada de esto toca lo que usa el guion de la demo: los casos son anteriores a los de la demo y estan
// resueltos, asi que no aparecen como pendientes ni quedan antes en los listados
final class HistoriaReportes {

    static final int ANIO = 2026;
    static final LocalDate PRIMER_DIA = LocalDate.of(ANIO, 1, 5);
    static final LocalDate ULTIMO_DIA = LocalDate.of(ANIO, 10, 31);
    static final int CAMIONES_POR_EMPRESA = 2;
    // las matriculas de los camiones nuevos: SF, la letra de la empresa y 310 mas el numero de camion (SFA3101)
    static final String PREFIJO_MATRICULAS = "SF_310";
    private static final LocalDate ALTA = LocalDate.of(2024, 1, 1);
    private static final String FUNCIONARIO = "11111111";

    record Camion(String matricula, String marca, String modelo, int peso, int capacidad) {
    }

    private static final String[][] MODELOS = {
            { "Volvo", "FH 460" }, { "Scania", "R 450" }, { "Mercedes-Benz", "Actros 2645" }, { "Iveco", "Stralis 480" },
            { "DAF", "XF 450" } };

    // los camiones nuevos de cada empresa de HistoriaPublica (SFA3101 y SFA3102 para la primera, y asi)
    static List<Camion> camiones(int empresa) {
        List<Camion> camiones = new ArrayList<>();
        for (int k = 1; k <= CAMIONES_POR_EMPRESA; k++) {
            String[] modelo = MODELOS[(empresa + k) % MODELOS.length];
            camiones.add(new Camion("SF" + (char) ('A' + empresa) + "310" + k, modelo[0], modelo[1], 8200 + 150 * k + 100 * empresa,
                    23000 + 500 * k));
        }
        return camiones;
    }

    // un viaje terminado: que camion (indice entre todos los de las empresas), que dia, rubro y tipo de carga (indices)
    record ViajeHistorico(int camion, LocalDate fecha, int rubro, int tipoCarga, int volumen) {
    }

    // el rubro de cada viaje sale de esta lista, asi unos rubros mueven mas carga que otros en el grafico de volumen
    private static final int[] PESO_RUBROS = { 0, 0, 0, 1, 1, 2, 3, 3, 4, 5 };

    // de dos a cinco viajes por dia habil (uno mas en la zafra) y alguno los sabados. Determinista: los mismos datos
    // en cada reinicio
    static List<ViajeHistorico> viajes(int camiones, int rubros, int tipos) {
        List<ViajeHistorico> viajes = new ArrayList<>();
        for (LocalDate dia = PRIMER_DIA; !dia.isAfter(ULTIMO_DIA); dia = dia.plusDays(1)) {
            int n = dia.getDayOfYear();
            int cantidad;
            if (dia.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
                cantidad = 0;
            } else if (dia.getDayOfWeek().equals(DayOfWeek.SATURDAY)) {
                cantidad = n % 2;
            } else {
                cantidad = 2 + (n * 7) % 3 + (dia.getMonthValue() >= 3 && dia.getMonthValue() <= 5 ? 1 : 0);
            }
            for (int i = 0; i < Math.min(cantidad, camiones); i++) {
                int rubro = PESO_RUBROS[(n + 7 * i) % PESO_RUBROS.length] % rubros;
                viajes.add(new ViajeHistorico((n * 3 + i) % camiones, dia, rubro, (n + 3 * i) % tipos,
                        8000 + ((n * 13 + i * 29) % 14) * 1000));
            }
        }
        return viajes;
    }

    // uno de cada nueve viajes tiene un caso. Tipos: exceso de carga, desvio, sin habilitacion y viaje sin guia, en esa
    // proporcion aproximada; siete de cada diez se confirman (salvo los sin habilitacion) y uno de cada tres tiene descargo
    static TipoInfraccion infraccion(int viaje) {
        if (viaje % 9 != 4) {
            return null;
        }
        return switch ((viaje / 9) % 7) {
            case 0, 1, 2 -> TipoInfraccion.EXCESO_CARGA;
            case 3, 4 -> TipoInfraccion.DESVIO_RUTA;
            case 5 -> TipoInfraccion.SIN_HABILITACION;
            default -> TipoInfraccion.VIAJE_SIN_GUIA;
        };
    }

    private final EmpresaDAOLocal empresaDAO;
    private final VehiculoDAOLocal vehiculoDAO;
    private final PermisoDAOLocal permisoDAO;
    private final ItvDAOLocal itvDAO;
    private final UsuarioDAOLocal usuarioDAO;
    private final GuiaDAOLocal guiaDAO;
    private final ViajeDAOLocal viajeDAO;
    private final CasoFiscalizacionDAOLocal casoDAO;

    HistoriaReportes(EmpresaDAOLocal empresaDAO, VehiculoDAOLocal vehiculoDAO, PermisoDAOLocal permisoDAO, ItvDAOLocal itvDAO,
            UsuarioDAOLocal usuarioDAO, GuiaDAOLocal guiaDAO, ViajeDAOLocal viajeDAO, CasoFiscalizacionDAOLocal casoDAO) {
        this.empresaDAO = empresaDAO;
        this.vehiculoDAO = vehiculoDAO;
        this.permisoDAO = permisoDAO;
        this.itvDAO = itvDAO;
        this.usuarioDAO = usuarioDAO;
        this.guiaDAO = guiaDAO;
        this.viajeDAO = viajeDAO;
        this.casoDAO = casoDAO;
    }

    // solo la primera vez, y despues de HistoriaPublica: si el primer camion nuevo ya esta, se cargo todo junto
    void cargar() {
        if (vehiculoDAO.getVehiculoByMatricula(camiones(0).get(0).matricula()) != null) {
            return;
        }
        List<Long> camiones = new ArrayList<>();
        List<Camion> datosCamiones = new ArrayList<>();
        List<Long> empresas = new ArrayList<>();
        List<Long> personas = new ArrayList<>();
        for (int e = 0; e < HistoriaPublica.EMPRESAS.size(); e++) {
            HistoriaPublica.EmpresaPublica datos = HistoriaPublica.EMPRESAS.get(e);
            EmpresaDto empresa = empresaDAO.getEmpresaByNro(datos.nro());
            UsuarioDto persona = usuarioDAO.getCiudadanoByCedula(datos.cedula());
            if (empresa == null || persona == null) {
                return;
            }
            for (Camion camion : camiones(e)) {
                vehiculoDAO.addVehiculo(new VehiculoDto(camion.matricula(), camion.marca(), camion.modelo(), camion.peso(),
                        camion.capacidad(), empresa.getId()));
                Long id = vehiculoDAO.getVehiculoByMatricula(camion.matricula()).getId();
                permisoDAO.addPermiso(new PermisoDto("PNC-" + camion.matricula().substring(2), LocalDate.of(ANIO, 1, 1),
                        LocalDate.of(ANIO, 12, 31), id, EstadoPermiso.APROBADO));
                itvDAO.addItv(new ItvDto(id, LocalDate.of(ANIO, 1, 3), LocalDate.of(ANIO + 1, 1, 3)));
                camiones.add(id);
                datosCamiones.add(camion);
                empresas.add(empresa.getId());
                personas.add(persona.getId());
            }
        }
        List<RubroDto> rubros = guiaDAO.getRubros();
        List<TipoCargaDto> tipos = guiaDAO.getTiposCarga();
        UsuarioDto funcionario = usuarioDAO.getFuncionarioByCedula(FUNCIONARIO);
        List<ViajeHistorico> viajes = viajes(camiones.size(), rubros.size(), tipos.size());
        for (int j = 0; j < viajes.size(); j++) {
            ViajeHistorico viaje = viajes.get(j);
            int c = viaje.camion();
            LocalDateTime salida = viaje.fecha().atTime(6 + j % 5, 0);
            GuiaDto guia = guiaDAO.addGuia(new GuiaDto(viaje.fecha(), -34.9011, -56.1645, -31.3833, -57.9667, viaje.volumen(),
                    empresas.get(c), personas.get(c), rubros.get(viaje.rubro()).getId(), tipos.get(viaje.tipoCarga()).getId()));
            Long id = viajeDAO.addViaje(new ViajeDto(guia.getId(), camiones.get(c), personas.get(c))).getId();
            viajeDAO.cambiarEstado(id, EstadoViaje.EN_CURSO, salida);
            viajeDAO.cambiarEstado(id, EstadoViaje.FINALIZADO, salida.plusHours(8));
            TipoInfraccion tipo = infraccion(j);
            if (tipo != null && funcionario != null) {
                // el viaje sin guia es de esa noche, despues de terminado el viaje del dia: no tiene viaje asociado
                boolean sinGuia = tipo == TipoInfraccion.VIAJE_SIN_GUIA;
                agregarCaso(j, tipo, camiones.get(c), datosCamiones.get(c), sinGuia ? null : id,
                        salida.plusHours(sinGuia ? 12 : 3), personas.get(c), funcionario.getId());
            }
        }
    }

    private void agregarCaso(int j, TipoInfraccion tipo, Long idVehiculo, Camion camion, Long idViaje, LocalDateTime hecho,
            Long responsable, Long funcionario) {
        CasoFiscalizacionDto caso = new CasoFiscalizacionDto(tipo, tipo + "|" + idVehiculo + "|" + hecho, hecho, idVehiculo, idViaje);
        Long id = casoDAO.addCaso(caso, List.of(evidencia(j, tipo, camion, hecho)), hecho.plusMinutes(5)).getId();
        LocalDateTime notificacion = hecho.plusMinutes(30);
        int plazo = casoDAO.getNormaVigente(NormaFiscalizacion.PLAZO_DESCARGO_DIAS, notificacion.toLocalDate());
        casoDAO.notificar(id, notificacion, notificacion.plusDays(plazo));
        // los camiones tienen la ITV vigente todo el anio, asi que los casos sin habilitacion se desestiman
        boolean confirmada = j % 10 < 7 && tipo != TipoInfraccion.SIN_HABILITACION;
        // j / 9: los casos son los viajes con j % 9 == 4, asi que j % 3 seria siempre 1
        if ((j / 9) % 3 == 0) {
            casoDAO.presentarDescargo(id, responsable, confirmada
                    ? "Pedimos revisar el registro; no tenemos constancia del hecho."
                    : "Adjuntamos la documentacion que muestra que el hecho no corresponde.", notificacion.plusDays(2));
        }
        casoDAO.resolver(id, funcionario, confirmada ? EstadoCaso.CONFIRMADA : EstadoCaso.DESESTIMADA,
                confirmada ? "La evidencia acredita la infraccion." : "La evidencia no alcanza para confirmar la infraccion.",
                notificacion.plusDays(5 + j % 9));
    }

    private static EvidenciaDto evidencia(int j, TipoInfraccion tipo, Camion camion, LocalDateTime hecho) {
        return switch (tipo) {
            case EXCESO_CARGA -> {
                int maximo = camion.peso() + camion.capacidad();
                yield new EvidenciaDto(TipoEvidencia.PESAJE, "balanzas-sur/balanza-r5-km30/h" + j, hecho,
                        (maximo + 600 + (j % 7) * 400) + " kg", maximo + " kg", "peso del vehiculo + capacidad declarada",
                        "Pesada en la balanza de ruta 5 km 30 por encima del maximo.");
            }
            case DESVIO_RUTA -> new EvidenciaDto(TipoEvidencia.POSICION, "tracking-1/historia-" + j, hecho,
                    (6000 + (j % 11) * 900) + " m", "0 m", "5000 m", "Posicion fuera del tramo entre el origen y el destino de la Guia.");
            case SIN_HABILITACION -> new EvidenciaDto(TipoEvidencia.ITV, "itv-" + camion.matricula().toLowerCase(), hecho, "vencida",
                    "vigente", null, "El vehiculo viajo con la ITV vencida segun el registro.");
            case VIAJE_SIN_GUIA -> new EvidenciaDto(TipoEvidencia.POSICION, "tracking-2/historia-" + j, hecho, "en movimiento",
                    "sin viaje", null, "El vehiculo circulo sin un viaje asignado.");
        };
    }
}
