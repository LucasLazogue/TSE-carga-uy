package tse.cargauy.data.testdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tse.cargauy.data.testdata.DaosFalsos.conId;
import static tse.cargauy.data.testdata.DaosFalsos.falso;
import static tse.cargauy.data.testdata.DaosFalsos.lista;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

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
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.entities.TipoEvidencia;
import tse.cargauy.entities.TipoInfraccion;

class HistoriaReportesTest {

    private static final int RUBROS = 6;
    private static final int TIPOS = 6;
    private static final int CAMIONES = HistoriaPublica.EMPRESAS.size() * HistoriaReportes.CAMIONES_POR_EMPRESA;

    @Test
    void hayViajesCasiTodosLosDiasHabilesYNingunCamionViajaDosVecesElMismoDia() {
        List<HistoriaReportes.ViajeHistorico> viajes = HistoriaReportes.viajes(CAMIONES, RUBROS, TIPOS);

        Map<LocalDate, Set<Integer>> camionesPorDia = new HashMap<>();
        Set<Integer> rubros = new HashSet<>();
        for (HistoriaReportes.ViajeHistorico viaje : viajes) {
            assertTrue(camionesPorDia.computeIfAbsent(viaje.fecha(), d -> new HashSet<>()).add(viaje.camion()));
            assertTrue(!viaje.fecha().isBefore(HistoriaReportes.PRIMER_DIA) && !viaje.fecha().isAfter(HistoriaReportes.ULTIMO_DIA));
            assertTrue(viaje.fecha().getDayOfWeek().getValue() < 7);
            assertTrue(viaje.tipoCarga() >= 0 && viaje.tipoCarga() < TIPOS);
            assertTrue(viaje.volumen() >= 8000 && viaje.volumen() <= 21000);
            rubros.add(viaje.rubro());
        }
        assertEquals(RUBROS, rubros.size());
        // todos los dias habiles del periodo tienen al menos dos viajes
        for (LocalDate dia = HistoriaReportes.PRIMER_DIA; !dia.isAfter(HistoriaReportes.ULTIMO_DIA); dia = dia.plusDays(1)) {
            if (dia.getDayOfWeek().getValue() <= 5) {
                assertTrue(camionesPorDia.get(dia).size() >= 2, dia.toString());
            }
        }
        assertTrue(viajes.size() > 500);
    }

    @Test
    void losCamionesNuevosNoChocanConLosDeLaDemo() {
        Set<String> matriculas = new HashSet<>();
        for (int e = 0; e < HistoriaPublica.EMPRESAS.size(); e++) {
            for (HistoriaReportes.Camion camion : HistoriaReportes.camiones(e)) {
                assertTrue(matriculas.add(camion.matricula()));
                assertTrue(camion.matricula().matches(HistoriaReportes.PREFIJO_MATRICULAS.replace("_", ".") + "\\d"));
            }
        }
        // las matriculas de HistoriaPublica (SFA2101...) y la que se crea en vivo en la demo (SEE7890)
        HistoriaPublica.EMPRESAS.forEach(e -> assertTrue(!matriculas.contains(e.matricula())));
        assertTrue(!matriculas.contains("SEE7890"));
    }

    @Test
    void cargaCamionesViajesYCasosResueltosUnaSolaVez() {
        Base base = new Base(true);
        HistoriaReportes historia = base.historia();

        historia.cargar();
        historia.cargar();

        int viajes = HistoriaReportes.viajes(CAMIONES, RUBROS, TIPOS).size();
        assertEquals(CAMIONES, base.vehiculos.size());
        assertEquals(CAMIONES, base.permisos);
        assertEquals(CAMIONES, base.itvs);
        assertEquals(viajes, base.guias.size());
        assertEquals(viajes, base.viajes.size());
        assertEquals(viajes * 2, base.cambiosDeEstado);
        assertTrue(base.casos.size() > 50);
        assertEquals(base.casos.size(), base.notificados);
        assertEquals(base.casos.size(), base.resoluciones.size());
        assertTrue(base.descargos > 0);

        Map<TipoInfraccion, Integer> porTipo = new EnumMap<>(TipoInfraccion.class);
        for (CasoFiscalizacionDto caso : base.casos) {
            porTipo.merge(caso.getTipo(), 1, Integer::sum);
            // el viaje sin guia no tiene viaje; los otros si, asi cuentan al filtrar por tipo de carga
            if (caso.getTipo() == TipoInfraccion.VIAJE_SIN_GUIA) {
                assertNull(caso.getIdViaje());
            } else {
                assertNotNull(caso.getIdViaje());
            }
        }
        assertEquals(TipoInfraccion.values().length, porTipo.size());
        assertTrue(base.resoluciones.contains(EstadoCaso.CONFIRMADA));
        assertTrue(base.resoluciones.contains(EstadoCaso.DESESTIMADA));
        // el desvio y el viaje sin guia usan POSICION; el exceso, PESAJE; sin habilitacion, ITV
        assertEquals(Set.of(TipoEvidencia.PESAJE, TipoEvidencia.POSICION, TipoEvidencia.ITV),
                new HashSet<>(base.evidencias.stream().map(EvidenciaDto::getTipo).toList()));
    }

    @Test
    void sinLasEmpresasDeLaInformacionPublicaNoCargaNada() {
        Base base = new Base(false);

        base.historia().cargar();

        assertEquals(0, base.vehiculos.size());
        assertEquals(0, base.guias.size());
    }

    private static final class Base {
        final boolean conEmpresas;
        final Map<String, VehiculoDto> vehiculos = new HashMap<>();
        final List<GuiaDto> guias = new ArrayList<>();
        final List<ViajeDto> viajes = new ArrayList<>();
        final List<CasoFiscalizacionDto> casos = new ArrayList<>();
        final List<EvidenciaDto> evidencias = new ArrayList<>();
        final List<EstadoCaso> resoluciones = new ArrayList<>();
        int permisos;
        int itvs;
        int cambiosDeEstado;
        int notificados;
        int descargos;
        long ids = 100;

        Base(boolean conEmpresas) {
            this.conEmpresas = conEmpresas;
        }

        HistoriaReportes historia() {
            return new HistoriaReportes(
                    falso(EmpresaDAOLocal.class, Map.of("getEmpresaByNro", a -> conEmpresas ? conId(new EmpresaDto(), ids++) : null)),
                    falso(VehiculoDAOLocal.class, Map.of(
                            "addVehiculo", a -> {
                                VehiculoDto vehiculo = (VehiculoDto) a[0];
                                vehiculo.setId(ids++);
                                return vehiculos.put(vehiculo.getMatricula(), vehiculo);
                            },
                            "getVehiculoByMatricula", a -> vehiculos.get(a[0]))),
                    falso(PermisoDAOLocal.class, Map.of("addPermiso", a -> permisos++)),
                    falso(ItvDAOLocal.class, Map.of("addItv", a -> itvs++)),
                    falso(UsuarioDAOLocal.class, Map.of(
                            "getCiudadanoByCedula", a -> conId(new UsuarioDto(), ids++),
                            "getFuncionarioByCedula", a -> conId(new UsuarioDto(), 1L))),
                    falso(GuiaDAOLocal.class, Map.of(
                            "getRubros", a -> lista(RUBROS, i -> conId(new RubroDto("rubro" + i), i)),
                            "getTiposCarga", a -> lista(TIPOS, i -> conId(new TipoCargaDto("tipo" + i), i)),
                            "addGuia", a -> {
                                GuiaDto guia = (GuiaDto) a[0];
                                guia.setId(ids++);
                                guias.add(guia);
                                return guia;
                            })),
                    falso(ViajeDAOLocal.class, Map.of(
                            "addViaje", a -> {
                                ViajeDto viaje = (ViajeDto) a[0];
                                viaje.setId(ids++);
                                viajes.add(viaje);
                                return viaje;
                            },
                            "cambiarEstado", a -> cambiosDeEstado++)),
                    falso(CasoFiscalizacionDAOLocal.class, Map.of(
                            "addCaso", a -> {
                                CasoFiscalizacionDto caso = (CasoFiscalizacionDto) a[0];
                                caso.setId(ids++);
                                casos.add(caso);
                                @SuppressWarnings("unchecked")
                                List<EvidenciaDto> lasEvidencias = (List<EvidenciaDto>) a[1];
                                evidencias.addAll(lasEvidencias);
                                return caso;
                            },
                            "getNormaVigente", a -> 10,
                            "notificar", a -> notificados++,
                            "presentarDescargo", a -> descargos++,
                            "resolver", a -> resoluciones.add((EstadoCaso) a[2]))));
        }
    }
}
