package tse.cargauy.negocio.chofer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import tse.cargauy.data.empresa.EmpresaDAO;
import tse.cargauy.data.usuario.UsuarioDAO;
import tse.cargauy.data.viaje.ViajeDAO;
import tse.cargauy.dtos.ChoferDto;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.FiltroChoferes;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoViaje;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.exceptions.MensajesError;
import tse.cargauy.negocio.empresa.EmpresaEJB;

class ChoferEJBTest {

    private static final Long EMPRESA = 10L;
    private static final Long OTRA_EMPRESA = 20L;
    private static final Long EMPRESA_INEXISTENTE = 99L;
    private static final String FUNCIONARIO = "11111111";
    private static final String CIUDADANO = "22222222";
    private static final String CEDULA_NUEVA = "12345672";
    private static final LocalDate HOY = LocalDate.now();

    private final Map<String, Long> ciudadanos = new HashMap<>(Map.of(CIUDADANO, 5L));
    private final List<Vinculo> vinculos = new ArrayList<>();
    private final Map<Long, List<ViajeDto>> pendientes = new HashMap<>();
    private final ChoferEJB ejb = new ChoferEJB();

    // lo que la base guarda de cada vinculo; ChoferDto no lleva la empresa
    private class Vinculo {
        Long id;
        Long idCiudadano;
        Long idEmpresa;
        LocalDate desde;
        LocalDate hasta;

        ChoferDto toDto() {
            ChoferDto dto = new ChoferDto();
            dto.setId(idCiudadano);
            dto.setIdVinculo(id);
            dto.setCedula(ciudadanos.entrySet().stream()
                    .filter(c -> c.getValue().equals(idCiudadano))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElseThrow());
            dto.setFechaDesde(desde);
            dto.setFechaHasta(hasta);
            return dto;
        }
    }

    @BeforeEach
    void iniciar() {
        ejb.empresaEJB = new EmpresaEJB() {
            @Override
            public void validarAcceso(Long idEmpresa) {
            }
        };
        ejb.empresaDAO = new EmpresaDAO() {
            @Override
            public EmpresaDto getEmpresaById(Long id) {
                return EMPRESA_INEXISTENTE.equals(id) ? null : new EmpresaDto();
            }
        };
        ejb.viajeDAO = new ViajeDAO() {
            @Override
            public List<ViajeDto> getPendientesDelVinculo(Long idVinculo) {
                return pendientes.getOrDefault(idVinculo, List.of());
            }
        };
        ejb.usuarioDAO = new UsuarioDAO() {
            @Override
            public UsuarioDto getFuncionarioByCedula(String cedula) {
                return FUNCIONARIO.equals(cedula) ? new UsuarioDto() : null;
            }

            @Override
            public UsuarioDto getCiudadanoByCedula(String cedula) {
                return ciudadanos.containsKey(cedula) ? usuario(ciudadanos.get(cedula)) : null;
            }

            @Override
            public UsuarioDto addCiudadano(String cedula, String correo) {
                Long id = 100L + ciudadanos.size();
                ciudadanos.put(cedula, id);
                return usuario(id);
            }

            @Override
            public Long getChoferSinBaja(Long idCiudadano, Long idEmpresa, LocalDate fecha) {
                return vinculos.stream()
                        .filter(v -> v.idCiudadano.equals(idCiudadano) && v.idEmpresa.equals(idEmpresa))
                        .filter(v -> !v.desde.isAfter(fecha) && v.hasta == null)
                        .map(v -> v.id)
                        .findFirst()
                        .orElse(null);
            }

            @Override
            public ChoferDto getVinculoSinBaja(Long idVinculo, Long idEmpresa) {
                return vinculos.stream()
                        .filter(v -> v.id.equals(idVinculo) && v.idEmpresa.equals(idEmpresa) && v.hasta == null)
                        .map(Vinculo::toDto)
                        .findFirst()
                        .orElse(null);
            }

            @Override
            public ChoferDto addChofer(Long idCiudadano, Long idEmpresa, LocalDate fechaDesde) {
                return vinculo(idCiudadano, idEmpresa, fechaDesde, null).toDto();
            }

            @Override
            public ChoferDto darDeBajaChofer(Long idVinculo, LocalDate fecha) {
                Vinculo vinculo = vinculos.stream().filter(v -> v.id.equals(idVinculo)).findFirst().orElseThrow();
                vinculo.hasta = fecha;
                return vinculo.toDto();
            }
        };
    }

    private Vinculo vinculo(Long idCiudadano, Long idEmpresa, LocalDate desde, LocalDate hasta) {
        Vinculo vinculo = new Vinculo();
        vinculo.id = 1000L + vinculos.size();
        vinculo.idCiudadano = idCiudadano;
        vinculo.idEmpresa = idEmpresa;
        vinculo.desde = desde;
        vinculo.hasta = hasta;
        vinculos.add(vinculo);
        return vinculo;
    }

    private static UsuarioDto usuario(Long id) {
        UsuarioDto usuario = new UsuarioDto();
        usuario.setId(id);
        return usuario;
    }

    private static ChoferDto chofer(String cedula) {
        ChoferDto chofer = new ChoferDto();
        chofer.setCedula(cedula);
        return chofer;
    }

    private static ViajeDto viaje(String nroGuia, EstadoViaje estado) {
        ViajeDto viaje = new ViajeDto();
        viaje.setNroGuia(nroGuia);
        viaje.setEstado(estado);
        return viaje;
    }

    private static CargaUYException assertCodigo(CodigoError esperado, Executable accion) {
        CargaUYException error = assertThrows(CargaUYException.class, accion);
        assertEquals(esperado, error.getCodigo());
        return error;
    }

    @Test
    void listarSinFechaUsaLaDeHoy() {
        ejb.usuarioDAO = new UsuarioDAO() {
            @Override
            public PaginaDto<ChoferDto> getChoferes(Long idEmpresa, FiltroChoferes filtro, Paginacion paginacion) {
                assertEquals(HOY, filtro.getFecha());
                return new PaginaDto<>(List.of(), 0, paginacion.getPagina(), paginacion.getTamanio());
            }
        };

        assertTrue(ejb.getChoferes(EMPRESA, new FiltroChoferes(), new Paginacion(0, 20)).getItems().isEmpty());
    }

    @Test
    void altaDeUnCiudadanoQueYaIngreso() {
        ChoferDto chofer = ejb.addChofer(EMPRESA, chofer(CIUDADANO));

        assertEquals(5L, chofer.getId());
        assertEquals(HOY, chofer.getFechaDesde());
        assertNull(chofer.getFechaHasta());
        assertEquals(1, vinculos.size());
        assertEquals(1, ciudadanos.size());
    }

    @Test
    void altaDeUnaCedulaQueNuncaIngresoCreaElCiudadano() {
        ChoferDto chofer = ejb.addChofer(EMPRESA, chofer(CEDULA_NUEVA));

        assertEquals(ciudadanos.get(CEDULA_NUEVA), chofer.getId());
        assertEquals(HOY, chofer.getFechaDesde());
    }

    @Test
    void altaEnUnaEmpresaInexistente() {
        assertCodigo(CodigoError.EMPRESA_NO_ENCONTRADA, () -> ejb.addChofer(EMPRESA_INEXISTENTE, chofer(CIUDADANO)));
        assertTrue(vinculos.isEmpty());
    }

    @Test
    void altaSinCedula() {
        assertCodigo(CodigoError.CHOFER_CEDULA_REQUERIDA, () -> ejb.addChofer(EMPRESA, null));
        assertCodigo(CodigoError.CHOFER_CEDULA_REQUERIDA, () -> ejb.addChofer(EMPRESA, chofer(null)));
        assertCodigo(CodigoError.CHOFER_CEDULA_REQUERIDA, () -> ejb.addChofer(EMPRESA, chofer(" ")));
        assertTrue(vinculos.isEmpty());
    }

    @Test
    void altaConCedulaInvalida() {
        assertCodigo(CodigoError.CEDULA_INVALIDA, () -> ejb.addChofer(EMPRESA, chofer("12345678")));
        assertTrue(vinculos.isEmpty());
    }

    @Test
    void unFuncionarioNoPuedeSerChofer() {
        assertCodigo(CodigoError.CHOFER_FUNCIONARIO, () -> ejb.addChofer(EMPRESA, chofer(FUNCIONARIO)));
        assertTrue(vinculos.isEmpty());
    }

    @Test
    void altaDeUnChoferYaActivo() {
        ejb.addChofer(EMPRESA, chofer(CIUDADANO));

        assertCodigo(CodigoError.CHOFER_YA_ACTIVO, () -> ejb.addChofer(EMPRESA, chofer(CIUDADANO)));
        assertEquals(1, vinculos.size());
    }

    @Test
    void unChoferPuedeEstarEnVariasEmpresas() {
        ejb.addChofer(EMPRESA, chofer(CIUDADANO));
        ejb.addChofer(OTRA_EMPRESA, chofer(CIUDADANO));

        assertEquals(2, vinculos.size());
    }

    @Test
    void reingresoTrasUnaBajaAnteriorCreaOtroVinculo() {
        vinculo(5L, EMPRESA, HOY.minusYears(1), HOY.minusDays(10));

        ChoferDto chofer = ejb.addChofer(EMPRESA, chofer(CIUDADANO));

        assertEquals(2, vinculos.size());
        assertEquals(HOY, chofer.getFechaDesde());
    }

    @Test
    void reingresoElMismoDiaDeLaBajaCreaOtroVinculo() {
        ChoferDto primero = ejb.addChofer(EMPRESA, chofer(CIUDADANO));
        ejb.darDeBaja(EMPRESA, primero.getIdVinculo());

        ChoferDto segundo = ejb.addChofer(EMPRESA, chofer(CIUDADANO));

        assertNotEquals(primero.getIdVinculo(), segundo.getIdVinculo());
        assertEquals(HOY, vinculos.get(0).hasta);
        assertNull(segundo.getFechaHasta());
    }

    @Test
    void bajaGuardaElDiaDeHoy() {
        Long idVinculo = vinculo(5L, EMPRESA, HOY.minusYears(1), null).id;

        ChoferDto chofer = ejb.darDeBaja(EMPRESA, idVinculo);

        assertEquals(HOY, chofer.getFechaHasta());
        assertEquals(HOY, vinculos.get(0).hasta);
    }

    @Test
    void bajaConUnViajeAsignado() {
        Long idVinculo = vinculo(5L, EMPRESA, HOY.minusYears(1), null).id;
        pendientes.put(idVinculo, List.of(viaje("12", EstadoViaje.ASIGNADO)));

        assertCodigo(CodigoError.CHOFER_CON_VIAJES_PENDIENTES, () -> ejb.darDeBaja(EMPRESA, idVinculo));
        assertNull(vinculos.get(0).hasta);
    }

    @Test
    void bajaConUnViajeEnCursoListaLasGuias() {
        Long idVinculo = vinculo(5L, EMPRESA, HOY.minusYears(1), null).id;
        pendientes.put(idVinculo, List.of(viaje("12", EstadoViaje.ASIGNADO), viaje("9", EstadoViaje.EN_CURSO)));

        CargaUYException error = assertCodigo(CodigoError.CHOFER_CON_VIAJES_PENDIENTES,
                () -> ejb.darDeBaja(EMPRESA, idVinculo));

        assertEquals("No se puede dar de baja al chofer 22222222: tiene viajes sin terminar (guias 12 ASIGNADO,"
                + " 9 EN_CURSO). Reasigne o elimine los asignados, y espere a que el chofer finalice los que estan"
                + " en curso.", MensajesError.resolver(error));
        assertNull(vinculos.get(0).hasta);
    }

    @Test
    void bajaDeUnVinculoYaDadoDeBaja() {
        Long idVinculo = vinculo(5L, EMPRESA, HOY.minusYears(1), HOY.minusDays(1)).id;

        assertCodigo(CodigoError.CHOFER_NO_ENCONTRADO, () -> ejb.darDeBaja(EMPRESA, idVinculo));
        assertEquals(HOY.minusDays(1), vinculos.get(0).hasta);
    }

    @Test
    void bajaDeUnVinculoDeOtraEmpresa() {
        Long idVinculo = vinculo(5L, OTRA_EMPRESA, HOY.minusYears(1), null).id;

        assertCodigo(CodigoError.CHOFER_NO_ENCONTRADO, () -> ejb.darDeBaja(EMPRESA, idVinculo));
        assertNull(vinculos.get(0).hasta);
    }
}
