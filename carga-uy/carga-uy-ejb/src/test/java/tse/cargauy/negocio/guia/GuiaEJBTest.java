package tse.cargauy.negocio.guia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ejb.SessionContext;
import tse.cargauy.data.guia.GuiaDAO;
import tse.cargauy.data.usuario.UsuarioDAO;
import tse.cargauy.data.viaje.ViajeDAO;
import tse.cargauy.dtos.FiltroGuias;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.RubroDto;
import tse.cargauy.dtos.TipoCargaDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoViaje;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.empresa.EmpresaEJB;

class GuiaEJBTest {

    private static final Long EMPRESA = 10L;
    private static final Long OTRA_EMPRESA = 20L;
    private static final Long GUIA = 1L;
    private static final Long RESPONSABLE = 5L;
    private static final Long RUBRO = 3L;
    private static final Long TIPO_CARGA = 4L;
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 1);

    private final GuiaEJB ejb = new GuiaEJB();
    private final List<String> llamadas = new ArrayList<>();

    private GuiaDto guardada;
    private ViajeDto viaje;
    private Long responsableVigente = RESPONSABLE;
    private Long empresaSinAcceso;

    @BeforeEach
    void setUp() {
        guardada = guia();
        guardada.setId(GUIA);
        guardada.setNroGuia("G-1");
        guardada.setIdEmpresa(EMPRESA);

        ejb.empresaEJB = new EmpresaEJB() {
            @Override
            public void validarAcceso(Long idEmpresa) {
                if (idEmpresa.equals(empresaSinAcceso)) {
                    throw new CargaUYException(CodigoError.ACCESO_DENEGADO);
                }
            }
        };
        ejb.contexto = (SessionContext) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { SessionContext.class }, (proxy, metodo, args) ->
                        metodo.getName().equals("getCallerPrincipal") ? (Principal) () -> String.valueOf(RESPONSABLE) : null);
        ejb.usuarioDAO = new UsuarioDAO() {
            @Override
            public Long getResponsableVigente(Long idCiudadano, Long idEmpresa, LocalDate fecha) {
                return responsableVigente;
            }
        };
        ejb.viajeDAO = new ViajeDAO() {
            @Override
            public ViajeDto getByGuia(Long idGuia) {
                return viaje;
            }
        };
        ejb.guiaDAO = new GuiaDAO() {
            @Override
            public GuiaDto getGuiaById(Long id) {
                return GUIA.equals(id) ? guardada : null;
            }

            @Override
            public PaginaDto<GuiaDto> buscar(FiltroGuias filtro, Paginacion paginacion) {
                llamadas.add("buscar " + filtro.getIdsEmpresa());
                return new PaginaDto<>(List.of(guardada), 1, paginacion.getPagina(), paginacion.getTamanio());
            }

            @Override
            public GuiaDto addGuia(GuiaDto guiaDto) {
                llamadas.add("add");
                return guiaDto;
            }

            @Override
            public void updateGuia(Long id, GuiaDto guiaDto) {
                llamadas.add("update " + id);
            }

            @Override
            public void deleteGuia(Long id) {
                llamadas.add("delete " + id);
            }

            @Override
            public RubroDto getRubroById(Long id) {
                return RUBRO.equals(id) ? new RubroDto("Granos") : null;
            }

            @Override
            public TipoCargaDto getTipoCargaById(Long id) {
                return TIPO_CARGA.equals(id) ? new TipoCargaDto() : null;
            }

            @Override
            public List<RubroDto> getRubros() {
                return List.of(new RubroDto("Granos"));
            }

            @Override
            public List<TipoCargaDto> getTiposCarga() {
                return List.of(new TipoCargaDto());
            }
        };
    }

    private static GuiaDto guia() {
        return new GuiaDto(FECHA, -34.9, -56.1, -34.4, -57.8, 1000, null, null, RUBRO, TIPO_CARGA);
    }

    private void assertCodigo(CodigoError esperado, Runnable accion) {
        CargaUYException e = assertThrows(CargaUYException.class, accion::run);
        assertEquals(esperado, e.getCodigo());
    }

    private void assertInvalida(CodigoError esperado, Consumer<GuiaDto> cambio) {
        GuiaDto guia = guia();
        cambio.accept(guia);
        assertCodigo(esperado, () -> ejb.addGuia(EMPRESA, guia));
        assertTrue(llamadas.isEmpty(), "no debe llegar al DAO");
    }

    @Test
    void listarFiltraPorLaEmpresaDelRecurso() {
        FiltroGuias filtro = new FiltroGuias();

        PaginaDto<GuiaDto> pagina = ejb.listar(EMPRESA, filtro, new Paginacion(0, 20));

        assertEquals(List.of("buscar " + List.of(EMPRESA)), llamadas);
        assertSame(guardada, pagina.getItems().get(0));
    }

    @Test
    void listarSinAccesoALaEmpresaNoConsulta() {
        empresaSinAcceso = OTRA_EMPRESA;

        assertCodigo(CodigoError.ACCESO_DENEGADO, () -> ejb.listar(OTRA_EMPRESA, new FiltroGuias(), new Paginacion(0, 20)));
        assertTrue(llamadas.isEmpty());
    }

    @Test
    void getGuiaDeLaEmpresa() {
        assertSame(guardada, ejb.getGuia(EMPRESA, GUIA));
    }

    // una guia de otra empresa se ve igual que una inexistente: no revela que existe
    @Test
    void getGuiaDeOtraEmpresaNoSeEncuentra() {
        assertCodigo(CodigoError.GUIA_NO_ENCONTRADA, () -> ejb.getGuia(OTRA_EMPRESA, GUIA));
        assertCodigo(CodigoError.GUIA_NO_ENCONTRADA, () -> ejb.getGuia(EMPRESA, 99L));
    }

    @Test
    void addGuiaLaRegistraAlResponsableQueLlama() {
        GuiaDto guia = guia();

        GuiaDto creada = ejb.addGuia(EMPRESA, guia);

        assertEquals(EMPRESA, creada.getIdEmpresa());
        assertEquals(RESPONSABLE, creada.getIdRegistradaPor());
        assertEquals(List.of("add"), llamadas);
    }

    @Test
    void addGuiaSinResponsableVigenteEnLaFecha() {
        responsableVigente = null;

        assertCodigo(CodigoError.GUIA_RESPONSABLE_INVALIDO, () -> ejb.addGuia(EMPRESA, guia()));
        assertTrue(llamadas.isEmpty());
    }

    @Test
    void addGuiaValidaCadaCampo() {
        assertInvalida(CodigoError.GUIA_FECHA_REQUERIDA, g -> g.setFecha(null));
        assertInvalida(CodigoError.GUIA_ORIGEN_REQUERIDO, g -> g.setOrigenLat(null));
        assertInvalida(CodigoError.GUIA_ORIGEN_REQUERIDO, g -> g.setOrigenLon(null));
        assertInvalida(CodigoError.GUIA_DESTINO_REQUERIDO, g -> g.setDestinoLat(null));
        assertInvalida(CodigoError.GUIA_DESTINO_REQUERIDO, g -> g.setDestinoLon(null));
        assertInvalida(CodigoError.GUIA_ORIGEN_IGUAL_DESTINO, g -> {
            g.setDestinoLat(g.getOrigenLat());
            g.setDestinoLon(g.getOrigenLon());
        });
        assertInvalida(CodigoError.GUIA_VOLUMEN_INVALIDO, g -> g.setVolumen(0));
        assertInvalida(CodigoError.GUIA_RUBRO_REQUERIDO, g -> g.setIdRubro(null));
        assertInvalida(CodigoError.RUBRO_NO_ENCONTRADO, g -> g.setIdRubro(99L));
        assertInvalida(CodigoError.GUIA_TIPO_CARGA_REQUERIDO, g -> g.setIdTipoCarga(null));
        assertInvalida(CodigoError.TIPO_CARGA_NO_ENCONTRADO, g -> g.setIdTipoCarga(99L));
    }

    // mismo origen y solo la latitud distinta no es "origen igual a destino"
    @Test
    void addGuiaConSoloUnaCoordenadaIgualEsValida() {
        GuiaDto guia = guia();
        guia.setDestinoLat(guia.getOrigenLat());

        ejb.addGuia(EMPRESA, guia);

        assertEquals(List.of("add"), llamadas);
    }

    @Test
    void updateGuiaSinViaje() {
        GuiaDto cambios = guia();
        cambios.setVolumen(2000);

        assertSame(guardada, ejb.updateGuia(EMPRESA, GUIA, cambios));
        assertEquals(List.of("update " + GUIA), llamadas);
    }

    @Test
    void updateGuiaConViajeAsignadoPermiteCambiarLoQueNoEsFijo() {
        viaje = viajeEn(EstadoViaje.ASIGNADO);
        GuiaDto cambios = guia();
        cambios.setDestinoLat(-33.0);

        ejb.updateGuia(EMPRESA, GUIA, cambios);

        assertEquals(List.of("update " + GUIA), llamadas);
    }

    @Test
    void updateGuiaConViajeAsignadoNoCambiaFechaNiVolumen() {
        viaje = viajeEn(EstadoViaje.ASIGNADO);
        GuiaDto otraFecha = guia();
        otraFecha.setFecha(FECHA.plusDays(1));
        GuiaDto otroVolumen = guia();
        otroVolumen.setVolumen(2000);

        assertCodigo(CodigoError.GUIA_ASIGNADA_DATOS_FIJOS, () -> ejb.updateGuia(EMPRESA, GUIA, otraFecha));
        assertCodigo(CodigoError.GUIA_ASIGNADA_DATOS_FIJOS, () -> ejb.updateGuia(EMPRESA, GUIA, otroVolumen));
        assertTrue(llamadas.isEmpty());
    }

    @Test
    void updateGuiaConViajeEnCursoNoSeModifica() {
        viaje = viajeEn(EstadoViaje.EN_CURSO);

        assertCodigo(CodigoError.GUIA_NO_MODIFICABLE, () -> ejb.updateGuia(EMPRESA, GUIA, guia()));
        assertTrue(llamadas.isEmpty());
    }

    @Test
    void updateGuiaDeOtraEmpresa() {
        assertCodigo(CodigoError.GUIA_NO_ENCONTRADA, () -> ejb.updateGuia(OTRA_EMPRESA, GUIA, guia()));
    }

    @Test
    void deleteGuiaSinViaje() {
        ejb.deleteGuia(EMPRESA, GUIA);

        assertEquals(List.of("delete " + GUIA), llamadas);
    }

    @Test
    void deleteGuiaConViajeNoSeBorra() {
        viaje = viajeEn(EstadoViaje.ASIGNADO);

        assertCodigo(CodigoError.GUIA_CON_VIAJE, () -> ejb.deleteGuia(EMPRESA, GUIA));
        assertTrue(llamadas.isEmpty());
    }

    @Test
    void catalogos() {
        assertEquals("Granos", ejb.getRubros().get(0).getNombre());
        assertEquals(1, ejb.getTiposCarga().size());
        assertNull(viaje);
    }

    private static ViajeDto viajeEn(EstadoViaje estado) {
        ViajeDto viaje = new ViajeDto();
        viaje.setEstado(estado);
        return viaje;
    }
}
