package tse.cargauy.data.testdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tse.cargauy.data.testdata.DaosFalsos.conId;
import static tse.cargauy.data.testdata.DaosFalsos.falso;
import static tse.cargauy.data.testdata.DaosFalsos.lista;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import tse.cargauy.data.empresa.EmpresaDAOLocal;
import tse.cargauy.data.guia.GuiaDAOLocal;
import tse.cargauy.data.itv.ItvDAOLocal;
import tse.cargauy.data.permiso.PermisoDAOLocal;
import tse.cargauy.data.usuario.UsuarioDAOLocal;
import tse.cargauy.data.vehiculo.VehiculoDAOLocal;
import tse.cargauy.data.viaje.ViajeDAOLocal;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoViaje;

class HistoriaPublicaTest {

    // el umbral de la informacion publica: empresas y vehiculos distintos por mes y por rubro
    private static final int UMBRAL = 5;
    private static final int RUBROS = 6;
    private static final int TIPOS = 6;

    @Test
    void cadaMesYCadaRubroSuperanElUmbral() {
        List<HistoriaPublica.ViajePublico> viajes = HistoriaPublica.viajes(HistoriaPublica.EMPRESAS.size(), RUBROS, TIPOS);

        Map<Integer, Set<Integer>> porMes = new HashMap<>();
        Map<Integer, Set<Integer>> porRubro = new HashMap<>();
        for (HistoriaPublica.ViajePublico viaje : viajes) {
            porMes.computeIfAbsent(viaje.mes(), m -> new HashSet<>()).add(viaje.empresa());
            porRubro.computeIfAbsent(viaje.rubro(), r -> new HashSet<>()).add(viaje.empresa());
            assertTrue(viaje.tipoCarga() >= 0 && viaje.tipoCarga() < TIPOS);
        }
        // cada empresa tiene un solo vehiculo, asi que empresas distintas son tambien vehiculos distintos
        assertEquals(HistoriaPublica.ULTIMO_MES - HistoriaPublica.PRIMER_MES + 1, porMes.size());
        porMes.values().forEach(empresas -> assertTrue(empresas.size() >= UMBRAL));
        assertEquals(RUBROS, porRubro.size());
        porRubro.values().forEach(empresas -> assertTrue(empresas.size() >= UMBRAL));
    }

    @Test
    void cargaLasEmpresasYSusViajesUnaSolaVez() {
        Base base = new Base();
        HistoriaPublica historia = base.historia();

        historia.cargar();
        historia.cargar();

        int empresas = HistoriaPublica.EMPRESAS.size();
        int meses = HistoriaPublica.ULTIMO_MES - HistoriaPublica.PRIMER_MES + 1;
        assertEquals(empresas, base.empresas.size());
        assertEquals(empresas, base.vehiculos.size());
        assertEquals(empresas, base.permisos);
        assertEquals(empresas, base.itvs);
        assertEquals(empresas, base.responsables);
        assertEquals(empresas, base.choferes);
        assertEquals(empresas * meses, base.guias.size());
        assertEquals(empresas * meses * 2, base.cambiosDeEstado.size());
        assertEquals(EstadoViaje.FINALIZADO, base.cambiosDeEstado.get(base.cambiosDeEstado.size() - 1));
        // dentro del anio y antes de hoy en la demo: febrero a octubre
        base.guias.forEach(guia -> assertTrue(guia.getFecha().isAfter(LocalDate.of(2026, 1, 31))
                && guia.getFecha().isBefore(LocalDate.of(2026, 11, 1))));
    }

    // DAOs falsos en memoria
    private static final class Base {
        final Map<Integer, EmpresaDto> empresas = new HashMap<>();
        final Map<String, VehiculoDto> vehiculos = new HashMap<>();
        final List<GuiaDto> guias = new ArrayList<>();
        final List<EstadoViaje> cambiosDeEstado = new ArrayList<>();
        int permisos;
        int itvs;
        int responsables;
        int choferes;
        long ids = 100;

        HistoriaPublica historia() {
            return new HistoriaPublica(
                    falso(EmpresaDAOLocal.class, Map.of(
                            "getEmpresaByNro", a -> empresas.get((Integer) a[0]),
                            "addEmpresa", a -> {
                                EmpresaDto empresa = (EmpresaDto) a[0];
                                empresa.setId(ids++);
                                empresas.put(empresa.getNroEmpresa(), empresa);
                                return empresa;
                            })),
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
                            "addCiudadano", a -> {
                                UsuarioDto persona = new UsuarioDto();
                                persona.setId(ids++);
                                return persona;
                            },
                            "addResponsable", a -> responsables++,
                            "addChofer", a -> {
                                choferes++;
                                return null;
                            })),
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
                                return viaje;
                            },
                            "cambiarEstado", a -> cambiosDeEstado.add((EstadoViaje) a[1]))));
        }
    }
}
