package tse.cargauy.data.testdata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
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
import tse.cargauy.dtos.FiltroCasos;
import tse.cargauy.dtos.FiltroViajes;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.ItvDto;
import tse.cargauy.dtos.Paginacion;
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
import tse.cargauy.entities.Rol;
import tse.cargauy.entities.TipoEvidencia;
import tse.cargauy.entities.TipoInfraccion;

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
    private ItvDAOLocal itvDAO;

    @EJB
    private UsuarioDAOLocal usuarioDAO;

    @EJB
    private GuiaDAOLocal guiaDAO;

    @EJB
    private ViajeDAOLocal viajeDAO;

    @EJB
    private CasoFiscalizacionDAOLocal casoDAO;

    @PostConstruct
    public void init() {
        // solo la primera vez, si no se duplican en cada arranque
        if (empresaDAO.getAll(new Paginacion(0, 3)).getTotal() == 0) {
            empresaDAO.addEmpresa(new EmpresaDto(1001, "Transportes del Sur", "Transportes del Sur S.A.", "Av. Italia 3456, Montevideo", LocalDate.of(2020, 3, 15)));
            empresaDAO.addEmpresa(new EmpresaDto(1002, "Cargas Norte", "Cargas del Norte S.R.L.", "Ruta 5 Km 480, Rivera", LocalDate.of(2019, 7, 1)));
            empresaDAO.addEmpresa(new EmpresaDto(1003, "LogiEste", "Logistica del Este Ltda.", "Av. Roosevelt 1200, Maldonado", LocalDate.of(2021, 11, 20)));
        }

        List<EmpresaDto> empresas = empresaDAO.getAll(new Paginacion(0, 3)).getItems();
        if (vehiculoDAO.getAll(new Paginacion(0, 1)).getTotal() == 0 && empresas.size() >= 3) {
            vehiculoDAO.addVehiculo(new VehiculoDto("STA1234", "Volvo", "FH 460", 8500, 24000, empresas.get(0).getId()));
            vehiculoDAO.addVehiculo(new VehiculoDto("SBB5678", "Scania", "R 450", 9000, 26000, empresas.get(0).getId()));
            vehiculoDAO.addVehiculo(new VehiculoDto("SCC9012", "Mercedes-Benz", "Actros 2646", 8800, 25000, empresas.get(1).getId()));
            vehiculoDAO.addVehiculo(new VehiculoDto("SDD3456", "Iveco", "Stralis 480", 8200, 23000, empresas.get(2).getId()));
        }

        if (permisoDAO.getAll(new Paginacion(0, 1)).getTotal() == 0 && vehiculoDAO.getAll(new Paginacion(0, 1)).getTotal() >= 4) {
            Long sta = vehiculoDAO.getVehiculoByMatricula("STA1234").getId();
            Long sbb = vehiculoDAO.getVehiculoByMatricula("SBB5678").getId();
            Long scc = vehiculoDAO.getVehiculoByMatricula("SCC9012").getId();
            Long sdd = vehiculoDAO.getVehiculoByMatricula("SDD3456").getId();

            permisoDAO.addPermiso(new PermisoDto("PNC-1001", LocalDate.of(2024, 1, 1), LocalDate.of(2025, 12, 31), sta, EstadoPermiso.APROBADO));
            permisoDAO.addPermiso(new PermisoDto("PNC-1002", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), sta, EstadoPermiso.APROBADO));
            permisoDAO.addPermiso(new PermisoDto("PNC-1003", LocalDate.of(2026, 3, 1), LocalDate.of(2027, 2, 28), sbb, EstadoPermiso.APROBADO));
            permisoDAO.addPermiso(new PermisoDto("PNC-1004", LocalDate.of(2026, 2, 1), LocalDate.of(2027, 1, 31), scc, EstadoPermiso.APROBADO));
            permisoDAO.addPermiso(new PermisoDto("PNC-1005", LocalDate.of(2025, 6, 1), LocalDate.of(2026, 5, 31), sdd, EstadoPermiso.APROBADO));
        }

        // vigentes en las fechas de las guias de los viajes; la de SDD3456 vencio, igual que su permiso
        if (itvDAO.getAll(new Paginacion(0, 1)).getTotal() == 0 && vehiculoDAO.getAll(new Paginacion(0, 1)).getTotal() >= 4) {
            Long sta = vehiculoDAO.getVehiculoByMatricula("STA1234").getId();
            Long sbb = vehiculoDAO.getVehiculoByMatricula("SBB5678").getId();
            Long scc = vehiculoDAO.getVehiculoByMatricula("SCC9012").getId();
            Long sdd = vehiculoDAO.getVehiculoByMatricula("SDD3456").getId();

            itvDAO.addItv(new ItvDto(sta, LocalDate.of(2026, 1, 15), LocalDate.of(2027, 1, 15)));
            itvDAO.addItv(new ItvDto(sbb, LocalDate.of(2026, 1, 15), LocalDate.of(2027, 1, 15)));
            itvDAO.addItv(new ItvDto(scc, LocalDate.of(2026, 1, 15), LocalDate.of(2027, 1, 15)));
            itvDAO.addItv(new ItvDto(sdd, LocalDate.of(2025, 4, 10), LocalDate.of(2026, 4, 10)));
        }

        if (usuarioDAO.getRoles().isEmpty()) {
            usuarioDAO.addRol(Rol.CIUDADANO);
            usuarioDAO.addRol(Rol.FUNCIONARIO);
            usuarioDAO.addFuncionario("11111111");
        }
        for (String rol : List.of(Rol.AUTORIDAD, Rol.ADMINISTRADOR)) {
            if (!usuarioDAO.getRoles().contains(rol)) {
                usuarioDAO.addRol(rol);
            }
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

        UsuarioDto ana = usuarioDAO.getCiudadanoByCedula("22222222");
        if (ana == null) {
            ana = usuarioDAO.addCiudadano("22222222", "ana.pereira@example.com");
        }
        if (usuarioDAO.getResponsableVigente(ana.getId(), empresas.get(1).getId(), ingreso) == null) {
            usuarioDAO.addResponsable(ana.getId(), empresas.get(1).getId(), ingreso);
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
            Long sbb = vehiculoDAO.getVehiculoByMatricula("SBB5678").getId();

            GuiaDto montevideoRivera = guiaDAO.addGuia(new GuiaDto(LocalDate.of(2026, 10, 15),
                    -34.9011, -56.1645, -30.9053, -55.5508, 18000,
                    empresas.get(0).getId(), chofer, rubros.get(0).getId(), tipos.get(3).getId()));
            viajeDAO.addViaje(new ViajeDto(montevideoRivera.getId(), sta, chofer));

            // un segundo viaje para el mismo chofer: puede tener varios asignados, pero iniciar uno a la vez
            GuiaDto frayBentosMontevideo = guiaDAO.addGuia(new GuiaDto(LocalDate.of(2026, 10, 20),
                    -33.1325, -58.2956, -34.9058, -56.2132, 20000,
                    empresas.get(0).getId(), chofer, rubros.get(3).getId(), tipos.get(4).getId()));
            viajeDAO.addViaje(new ViajeDto(frayBentosMontevideo.getId(), sbb, chofer));
        }

        if (viajeDAO.getAll(new FiltroViajes(null, null, EstadoViaje.FINALIZADO), new Paginacion(0, 1)).getTotal() == 0
                && empresas.size() >= 3) {
            List<RubroDto> rubros = guiaDAO.getRubros();
            List<TipoCargaDto> tipos = guiaDAO.getTiposCarga();
            Long chofer = carlos.getId();
            Long sta = vehiculoDAO.getVehiculoByMatricula("STA1234").getId();
            Long sbb = vehiculoDAO.getVehiculoByMatricula("SBB5678").getId();
            for (int mes = 2; mes <= 9; mes++) {
                agregarViajeRealizado(LocalDate.of(2026, mes, 5), sta, chofer, empresas.get(0).getId(),
                        rubros.get(mes % rubros.size()).getId(), tipos.get(mes % tipos.size()).getId(), 12000 + mes * 1000);
                if (mes >= 3) {
                    agregarViajeRealizado(LocalDate.of(2026, mes, 20), sbb, chofer, empresas.get(0).getId(),
                            rubros.get((mes + 2) % rubros.size()).getId(), tipos.get((mes + 1) % tipos.size()).getId(), 18000 + mes * 500);
                }
            }
        }

        if (casoDAO.getNormaVigente(NormaFiscalizacion.PLAZO_DESCARGO_DIAS, LocalDate.now()) == null) {
            casoDAO.addNorma(NormaFiscalizacion.PLAZO_DESCARGO_DIAS, 10, LocalDate.of(2026, 1, 1));
        }
        if (casoDAO.getNormaVigente(NormaFiscalizacion.TOLERANCIA_EXCESO_CARGA, LocalDate.now()) == null) {
            casoDAO.addNorma(NormaFiscalizacion.TOLERANCIA_EXCESO_CARGA, 5, LocalDate.of(2026, 1, 1));
        }
        if (casoDAO.getNormaVigente(NormaFiscalizacion.DESVIO_RUTA_DISTANCIA, LocalDate.now()) == null) {
            casoDAO.addNorma(NormaFiscalizacion.DESVIO_RUTA_DISTANCIA, 2000, LocalDate.of(2026, 1, 1));
        }

        // fechas relativas a hoy para que siempre haya un caso en plazo, uno vencido y uno en descargo
        if (casoDAO.buscar(new FiltroCasos(), new Paginacion(0, 1)).getTotal() == 0 && vehiculoDAO.getVehiculoByMatricula("SCC9012") != null) {
            LocalDateTime ahora = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
            Long scc = vehiculoDAO.getVehiculoByMatricula("SCC9012").getId();
            Long sbb = vehiculoDAO.getVehiculoByMatricula("SBB5678").getId();

            agregarCaso(TipoInfraccion.EXCESO_CARGA, scc, ahora.minusDays(3), ahora.minusDays(2),
                    new EvidenciaDto(TipoEvidencia.PESAJE, "balanzas-sur/balanza-r5-km30/1001", ahora.minusDays(3), "36500 kg", "33800 kg",
                            "peso del vehiculo + capacidad declarada", "Pesada en la balanza de ruta 5 km 30 por encima del maximo."));
            agregarCaso(TipoInfraccion.DESVIO_RUTA, scc, ahora.minusDays(16), ahora.minusDays(15),
                    new EvidenciaDto(TipoEvidencia.POSICION, "tracking-1/semilla-1", ahora.minusDays(16), "12,4 km", "0 km",
                            "2 km", "Posiciones a mas de 2 km del recorrido esperado durante 40 minutos."));
            Long enDescargo = agregarCaso(TipoInfraccion.EXCESO_CARGA, scc, ahora.minusDays(7), ahora.minusDays(6),
                    new EvidenciaDto(TipoEvidencia.PESAJE, "balanzas-sur/balanza-r1-km45/2002", ahora.minusDays(7), "35100 kg", "33800 kg",
                            "peso del vehiculo + capacidad declarada", "Pesada en la balanza de ruta 1 km 45 por encima del maximo."));
            agregarCaso(TipoInfraccion.VIAJE_SIN_GUIA, sbb, ahora.minusDays(2), ahora.minusDays(1),
                    new EvidenciaDto(TipoEvidencia.POSICION, "tracking-1/semilla-2", ahora.minusDays(2), "en movimiento", "sin viaje",
                            null, "El vehiculo circulo dos horas sin un viaje asignado."));

            casoDAO.presentarDescargo(enDescargo, ana.getId(),
                    "La balanza estaba descalibrada ese dia; adjuntamos el ticket del pesaje en planta.", ahora.minusDays(3));
        }

        if (casoDAO.buscar(new FiltroCasos(EstadoCaso.CONFIRMADA, null, null, null), new Paginacion(0, 1)).getTotal() == 0
                && vehiculoDAO.getVehiculoByMatricula("SCC9012") != null) {
            Long scc = vehiculoDAO.getVehiculoByMatricula("SCC9012").getId();
            Long sdd = vehiculoDAO.getVehiculoByMatricula("SDD3456").getId();
            Long funcionario = usuarioDAO.getFuncionarioByCedula("11111111").getId();

            Long exceso = agregarCaso(TipoInfraccion.EXCESO_CARGA, scc, LocalDateTime.of(2026, 4, 8, 10, 30), LocalDateTime.of(2026, 4, 8, 11, 0),
                    new EvidenciaDto(TipoEvidencia.PESAJE, "balanzas-norte/balanza-r5-km390/3003", LocalDateTime.of(2026, 4, 8, 10, 30),
                            "35900 kg", "33800 kg", "peso del vehiculo + capacidad declarada", "Pesada en la balanza de ruta 5 km 390 por encima del maximo."));
            casoDAO.resolver(exceso, funcionario, EstadoCaso.CONFIRMADA, "El pesaje supera el maximo y no hubo descargo.",
                    LocalDateTime.of(2026, 4, 25, 9, 0));
            Long desvio = agregarCaso(TipoInfraccion.DESVIO_RUTA, scc, LocalDateTime.of(2026, 6, 12, 15, 0), LocalDateTime.of(2026, 6, 12, 15, 30),
                    new EvidenciaDto(TipoEvidencia.POSICION, "tracking-1/semilla-3", LocalDateTime.of(2026, 6, 12, 15, 0), "8,1 km", "0 km",
                            "2 km", "Posiciones a mas de 2 km del recorrido esperado durante 25 minutos."));
            casoDAO.resolver(desvio, funcionario, EstadoCaso.CONFIRMADA, "El desvio no tiene justificacion.", LocalDateTime.of(2026, 6, 30, 9, 0));
            Long sinHabilitacion = agregarCaso(TipoInfraccion.SIN_HABILITACION, sdd, LocalDateTime.of(2026, 5, 3, 8, 0),
                    LocalDateTime.of(2026, 5, 3, 8, 30), new EvidenciaDto(TipoEvidencia.ITV, "itv-sdd3456", LocalDateTime.of(2026, 5, 3, 8, 0),
                            "vencida", "vigente", null, "La ITV del vehiculo vencio el 10/04/2026."));
            casoDAO.resolver(sinHabilitacion, funcionario, EstadoCaso.DESESTIMADA, "La ITV se renovo antes del viaje; el registro estaba atrasado.",
                    LocalDateTime.of(2026, 5, 20, 9, 0));
        }
    }

    private void agregarViajeRealizado(LocalDate fecha, Long idVehiculo, Long idChofer, Long idEmpresa, Long idRubro, Long idTipoCarga,
            int volumen) {
        GuiaDto guia = guiaDAO.addGuia(new GuiaDto(fecha, -34.9011, -56.1645, -30.9053, -55.5508, volumen, idEmpresa, idChofer, idRubro,
                idTipoCarga));
        Long idViaje = viajeDAO.addViaje(new ViajeDto(guia.getId(), idVehiculo, idChofer)).getId();
        viajeDAO.cambiarEstado(idViaje, EstadoViaje.EN_CURSO, fecha.atTime(8, 0));
        viajeDAO.cambiarEstado(idViaje, EstadoViaje.FINALIZADO, fecha.atTime(18, 0));
    }

    private Long agregarCaso(TipoInfraccion tipo, Long idVehiculo, LocalDateTime fechaHecho, LocalDateTime fechaNotificacion,
            EvidenciaDto evidencia) {
        CasoFiscalizacionDto caso = new CasoFiscalizacionDto(tipo, tipo + "|" + idVehiculo + "|" + fechaHecho, fechaHecho, idVehiculo, null);
        Long id = casoDAO.addCaso(caso, List.of(evidencia), fechaHecho.plusMinutes(5)).getId();
        int plazo = casoDAO.getNormaVigente(NormaFiscalizacion.PLAZO_DESCARGO_DIAS, fechaNotificacion.toLocalDate());
        casoDAO.notificar(id, fechaNotificacion, fechaNotificacion.plusDays(plazo));
        return id;
    }
}
