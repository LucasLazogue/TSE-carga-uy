package tse.cargauy.negocio.fiscalizacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import tse.cargauy.data.empresa.EmpresaDAO;
import tse.cargauy.data.usuario.UsuarioDAO;
import tse.cargauy.data.vehiculo.VehiculoDAO;
import tse.cargauy.dtos.CasoFiscalizacionDto;
import tse.cargauy.dtos.DescargoDto;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.EvidenciaDto;
import tse.cargauy.dtos.FiltroCasos;
import tse.cargauy.dtos.NotificacionDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.ResolucionDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.entities.AccionAuditoria;
import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.entities.TipoEvidencia;
import tse.cargauy.entities.TipoInfraccion;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;
import tse.cargauy.negocio.empresa.EmpresaEJB;

class CasoFiscalizacionEJBTest {

    private static final Long RESPONSABLE = 5001L;
    private static final Long FUNCIONARIO = 9001L;
    private static final Long EMPRESA = 10L;
    private static final Long OTRA_EMPRESA = 20L;
    private static final Long VEHICULO = 1L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 13, 10, 0);
    private static final Paginacion PAGINA = new Paginacion(0, 20);

    private final CasosEnMemoria casos = new CasosEnMemoria();
    private final CasoFiscalizacionEJB ejb = new CasoFiscalizacionEJB() {
        @Override
        Long idUsuario() {
            return usuario;
        }

        @Override
        LocalDateTime ahora() {
            return AHORA;
        }
    };
    private Long usuario = RESPONSABLE;

    @BeforeEach
    void preparar() {
        casos.empresaDeLosVehiculos = EMPRESA;
        ejb.casoDAO = casos;
        ejb.usuarioDAO = new UsuarioDAO() {
            @Override
            public Long getResponsableVigente(Long idCiudadano, Long idEmpresa, LocalDate fecha) {
                return RESPONSABLE.equals(idCiudadano) && EMPRESA.equals(idEmpresa) ? 1L : null;
            }
        };
        ejb.empresaDAO = new EmpresaDAO() {
            @Override
            public List<EmpresaDto> getByResponsable(Long idCiudadano, LocalDate fecha) {
                EmpresaDto empresa = new EmpresaDto();
                empresa.setId(EMPRESA);
                return RESPONSABLE.equals(idCiudadano) ? List.of(empresa) : List.of();
            }
        };
        ejb.empresaEJB = new EmpresaEJB() {
            @Override
            public void validarAcceso(Long idEmpresa) {
                if (!EMPRESA.equals(idEmpresa)) {
                    throw new CargaUYException(CodigoError.ACCESO_DENEGADO);
                }
            }
        };
        ejb.vehiculoDAO = new VehiculoDAO() {
            @Override
            public VehiculoDto getVehiculoById(Long id) {
                return VEHICULO.equals(id) ? new VehiculoDto() : null;
            }
        };
    }

    private static void rechaza(CodigoError codigo, Executable accion) {
        assertEquals(codigo, assertThrows(CargaUYException.class, accion).getCodigo());
    }

    private static CasoFiscalizacionDto hecho(String clave) {
        return new CasoFiscalizacionDto(TipoInfraccion.EXCESO_CARGA, clave, AHORA.minusHours(2), VEHICULO, null);
    }

    private static List<EvidenciaDto> evidencia() {
        return List.of(new EvidenciaDto(TipoEvidencia.PESAJE, "b1/1", AHORA.minusHours(2), "36500 kg", "33800 kg", null, null));
    }

    private List<AccionAuditoria> acciones(Long idCaso) {
        return casos.getAuditoria(idCaso).stream().map(p -> p.getAccion()).toList();
    }

    @Test
    void registrarCasoLoNotificaConElPlazoDeLaNorma() {
        CasoFiscalizacionDto caso = ejb.registrarCaso(hecho("h1"), evidencia());

        assertEquals(EstadoCaso.NOTIFICADA, caso.getEstado());
        assertEquals(AHORA, caso.getFechaNotificacion());
        assertEquals(AHORA.plusDays(10), caso.getVenceDescargo());
        assertEquals(List.of(AccionAuditoria.CREAR_CASO, AccionAuditoria.NOTIFICAR), acciones(caso.getId()));
        assertEquals(1, casos.notificaciones.size());
    }

    @Test
    void elMismoHechoNoGeneraOtroCaso() {
        CasoFiscalizacionDto primero = ejb.registrarCaso(hecho("h1"), evidencia());
        CasoFiscalizacionDto segundo = ejb.registrarCaso(hecho("h1"), evidencia());

        assertEquals(primero.getId(), segundo.getId());
        assertEquals(1, casos.casos.size());
        assertEquals(1, casos.notificaciones.size());
    }

    @Test
    void sinNormaVigenteNoSeRegistra() {
        casos.plazoDescargo = null;

        rechaza(CodigoError.NORMA_NO_VIGENTE, () -> ejb.registrarCaso(hecho("h1"), evidencia()));
    }

    @Test
    void unVehiculoDesconocidoNoGeneraCaso() {
        CasoFiscalizacionDto hecho = new CasoFiscalizacionDto(TipoInfraccion.EXCESO_CARGA, "h1", AHORA, 99L, null);

        rechaza(CodigoError.VEHICULO_NO_ENCONTRADO, () -> ejb.registrarCaso(hecho, evidencia()));
    }

    @Test
    void elResponsableSoloVeLasInfraccionesNotificadasDeSusEmpresas() {
        casos.agregar("a", EMPRESA, EstadoCaso.NOTIFICADA, AHORA.plusDays(5));
        casos.agregar("b", EMPRESA, EstadoCaso.DETECTADA, null);
        casos.agregar("c", OTRA_EMPRESA, EstadoCaso.NOTIFICADA, AHORA.plusDays(5));

        List<CasoFiscalizacionDto> infracciones = ejb.listarInfracciones(null, null, PAGINA).getItems();

        assertEquals(1, infracciones.size());
        assertEquals(EstadoCaso.NOTIFICADA, infracciones.get(0).getEstado());
        assertEquals(List.of(EMPRESA), casos.ultimoFiltro.getIdsEmpresa());
    }

    @Test
    void elResponsableNoListaInfraccionesDeOtraEmpresa() {
        rechaza(CodigoError.ACCESO_DENEGADO, () -> ejb.listarInfracciones(OTRA_EMPRESA, null, PAGINA));
    }

    @Test
    void elResponsableNoVeElCasoDeOtraEmpresa() {
        Long id = casos.agregar("c", OTRA_EMPRESA, EstadoCaso.NOTIFICADA, AHORA.plusDays(5)).getId();

        rechaza(CodigoError.CASO_NO_ENCONTRADO, () -> ejb.getInfraccion(id));
        rechaza(CodigoError.CASO_NO_ENCONTRADO, () -> ejb.presentarDescargo(id, new DescargoDto("no es mio")));
        assertEquals(EstadoCaso.NOTIFICADA, casos.getCasoById(id).getEstado());
    }

    @Test
    void unCasoDetectadoTodaviaNoEsVisibleParaLaEmpresa() {
        Long id = casos.agregar("b", EMPRESA, EstadoCaso.DETECTADA, null).getId();

        rechaza(CodigoError.CASO_NO_ENCONTRADO, () -> ejb.getInfraccion(id));
    }

    @Test
    void unCasoInexistenteNoSeEncuentra() {
        rechaza(CodigoError.CASO_NO_ENCONTRADO, () -> ejb.getInfraccion(99L));
        usuario = FUNCIONARIO;
        rechaza(CodigoError.CASO_NO_ENCONTRADO, () -> ejb.getCaso(99L));
        rechaza(CodigoError.CASO_NO_ENCONTRADO, () -> ejb.getAuditoria(99L));
    }

    @Test
    void elDescargoEnPlazoPasaElCasoAEnDescargoYQuedaAuditado() {
        Long id = ejb.registrarCaso(hecho("h1"), evidencia()).getId();

        CasoFiscalizacionDto caso = ejb.presentarDescargo(id, new DescargoDto("  la balanza estaba descalibrada  "));

        assertEquals(EstadoCaso.EN_DESCARGO, caso.getEstado());
        assertEquals("la balanza estaba descalibrada", caso.getDescargo().getTexto());
        assertEquals(List.of(AccionAuditoria.CREAR_CASO, AccionAuditoria.NOTIFICAR, AccionAuditoria.PRESENTAR_DESCARGO), acciones(id));
        assertEquals(String.valueOf(RESPONSABLE), casos.getAuditoria(id).get(2).getUsuario());
    }

    @Test
    void elSegundoDescargoSeRechaza() {
        Long id = ejb.registrarCaso(hecho("h1"), evidencia()).getId();
        ejb.presentarDescargo(id, new DescargoDto("primero"));

        rechaza(CodigoError.DESCARGO_YA_PRESENTADO, () -> ejb.presentarDescargo(id, new DescargoDto("segundo")));
        assertEquals("primero", casos.getCasoById(id).getDescargo().getTexto());
    }

    @Test
    void elDescargoFueraDePlazoSeRechazaSinCambiarElCaso() {
        Long id = casos.agregar("v", EMPRESA, EstadoCaso.NOTIFICADA, AHORA.minusMinutes(1)).getId();

        rechaza(CodigoError.DESCARGO_FUERA_DE_PLAZO, () -> ejb.presentarDescargo(id, new DescargoDto("tarde")));
        assertEquals(EstadoCaso.NOTIFICADA, casos.getCasoById(id).getEstado());
        assertEquals(List.of(), acciones(id));
    }

    @Test
    void sinDescargoNoSeResuelveAntesDelPlazo() {
        Long id = ejb.registrarCaso(hecho("h1"), evidencia()).getId();
        usuario = FUNCIONARIO;

        rechaza(CodigoError.CASO_PLAZO_NO_VENCIDO, () -> ejb.resolver(id, new ResolucionDto(EstadoCaso.CONFIRMADA, "sin descargo")));
        assertEquals(EstadoCaso.NOTIFICADA, casos.getCasoById(id).getEstado());
    }

    @Test
    void conElPlazoVencidoSeResuelveSinDescargo() {
        Long id = casos.agregar("v", EMPRESA, EstadoCaso.NOTIFICADA, AHORA.minusDays(1)).getId();
        usuario = FUNCIONARIO;

        CasoFiscalizacionDto caso = ejb.resolver(id, new ResolucionDto(EstadoCaso.DESESTIMADA, "la pesada no es concluyente"));

        assertEquals(EstadoCaso.DESESTIMADA, caso.getEstado());
        assertEquals(List.of(AccionAuditoria.DESESTIMAR), acciones(id));
    }

    @Test
    void laResolucionSinFundamentoSeRechaza() {
        Long id = casos.agregar("d", EMPRESA, EstadoCaso.EN_DESCARGO, AHORA.plusDays(3)).getId();
        usuario = FUNCIONARIO;

        rechaza(CodigoError.CASO_FUNDAMENTO_REQUERIDO, () -> ejb.resolver(id, new ResolucionDto(EstadoCaso.CONFIRMADA, "   ")));
        assertNull(casos.getCasoById(id).getResolucion());
    }

    @Test
    void confirmarConFundamentoCierraElCasoYQuedaAuditado() {
        Long id = ejb.registrarCaso(hecho("h1"), evidencia()).getId();
        ejb.presentarDescargo(id, new DescargoDto("descargo"));
        usuario = FUNCIONARIO;

        CasoFiscalizacionDto caso = ejb.resolver(id, new ResolucionDto(EstadoCaso.CONFIRMADA, " el ticket no coincide con la pesada "));

        assertEquals(EstadoCaso.CONFIRMADA, caso.getEstado());
        assertEquals("el ticket no coincide con la pesada", caso.getResolucion().getFundamento());
        assertEquals(AccionAuditoria.CONFIRMAR, acciones(id).get(3));
        assertEquals(String.valueOf(FUNCIONARIO), ejb.getAuditoria(id).get(3).getUsuario());
        rechaza(CodigoError.CASO_TRANSICION_INVALIDA, () -> ejb.resolver(id, new ResolucionDto(EstadoCaso.DESESTIMADA, "otra vez")));
    }

    @Test
    void laBandejaDelFuncionarioPasaLosFiltros() {
        casos.agregar("a", EMPRESA, EstadoCaso.NOTIFICADA, AHORA.plusDays(5));
        casos.agregar("b", OTRA_EMPRESA, EstadoCaso.EN_DESCARGO, AHORA.plusDays(5));
        FiltroCasos filtro = new FiltroCasos(EstadoCaso.EN_DESCARGO, null, null, null);

        List<CasoFiscalizacionDto> bandeja = ejb.listar(filtro, PAGINA).getItems();

        assertEquals(1, bandeja.size());
        assertSame(filtro, casos.ultimoFiltro);
    }

    @Test
    void laEmpresaRecibeLasNotificacionesDeSusCasos() {
        ejb.registrarCaso(hecho("h1"), evidencia());

        assertEquals(1, ejb.getNotificaciones(PAGINA).getTotal());
        usuario = 7777L;
        assertEquals(0, ejb.getNotificaciones(PAGINA).getTotal());
    }

    @Test
    void elDescargoLesLlegaALosFuncionarios() {
        Long id = ejb.registrarCaso(hecho("h1"), evidencia()).getId();
        ejb.presentarDescargo(id, new DescargoDto("descargo"));

        assertEquals(1, ejb.getNotificaciones(PAGINA).getTotal());
        usuario = FUNCIONARIO;
        List<NotificacionDto> avisos = ejb.getNotificacionesFuncionario(PAGINA).getItems();
        assertEquals(1, avisos.size());
        assertEquals(id, avisos.get(0).getIdCaso());
        assertFalse(avisos.get(0).isLeida());
    }

    @Test
    void laResolucionLeLlegaALaEmpresa() {
        Long id = casos.agregar("v", EMPRESA, EstadoCaso.NOTIFICADA, AHORA.minusDays(1)).getId();
        usuario = FUNCIONARIO;
        ejb.resolver(id, new ResolucionDto(EstadoCaso.CONFIRMADA, "fundamento"));

        assertEquals(0, ejb.getNotificacionesFuncionario(PAGINA).getTotal());
        usuario = RESPONSABLE;
        List<NotificacionDto> avisos = ejb.getNotificaciones(PAGINA).getItems();
        assertEquals(1, avisos.size());
        assertEquals(id, avisos.get(0).getIdCaso());
    }

    @Test
    void marcarLeidasSoloTocaLasDeQuienLasLee() {
        Long id = ejb.registrarCaso(hecho("h1"), evidencia()).getId();
        ejb.presentarDescargo(id, new DescargoDto("descargo"));

        ejb.marcarNotificacionesLeidas();

        assertTrue(ejb.getNotificaciones(PAGINA).getItems().get(0).isLeida());
        usuario = FUNCIONARIO;
        assertFalse(ejb.getNotificacionesFuncionario(PAGINA).getItems().get(0).isLeida());
        ejb.marcarNotificacionesFuncionarioLeidas();
        assertTrue(ejb.getNotificacionesFuncionario(PAGINA).getItems().get(0).isLeida());
    }

    @Test
    void laEmpresaNoVeQuienResolvioNiLaClaveDelHecho() {
        Long id = ejb.registrarCaso(hecho("h1"), evidencia()).getId();
        ejb.presentarDescargo(id, new DescargoDto("descargo"));
        usuario = FUNCIONARIO;
        ejb.resolver(id, new ResolucionDto(EstadoCaso.CONFIRMADA, "fundamento"));
        assertEquals(String.valueOf(FUNCIONARIO), ejb.getCaso(id).getResolucion().getCedulaFuncionario());
        usuario = RESPONSABLE;

        CasoFiscalizacionDto infraccion = ejb.getInfraccion(id);
        CasoFiscalizacionDto listada = ejb.listarInfracciones(null, null, PAGINA).getItems().get(0);

        assertNull(infraccion.getClaveDeduplicacion());
        assertNull(infraccion.getResolucion().getCedulaFuncionario());
        assertEquals("fundamento", infraccion.getResolucion().getFundamento());
        assertNull(listada.getClaveDeduplicacion());
        assertNull(listada.getResolucion().getCedulaFuncionario());
    }

    @Test
    void sinCuerpoNoSeResuelveNiSePresentaDescargo() {
        Long id = ejb.registrarCaso(hecho("h1"), evidencia()).getId();

        rechaza(CodigoError.PARAMETRO_INVALIDO, () -> ejb.presentarDescargo(id, null));
        usuario = FUNCIONARIO;
        rechaza(CodigoError.PARAMETRO_INVALIDO, () -> ejb.resolver(id, null));
    }
}
