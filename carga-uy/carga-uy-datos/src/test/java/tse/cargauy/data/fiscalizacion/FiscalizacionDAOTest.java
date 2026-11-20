package tse.cargauy.data.fiscalizacion;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.PersistenceException;
import tse.cargauy.data.BaseDatos;
import tse.cargauy.data.empresa.EmpresaDAO;
import tse.cargauy.data.informe.InformeDAO;
import tse.cargauy.data.usuario.UsuarioDAO;
import tse.cargauy.data.vehiculo.VehiculoDAO;
import tse.cargauy.dtos.CasoFiscalizacionDto;
import tse.cargauy.dtos.EvidenciaDto;
import tse.cargauy.dtos.FiltroCasos;
import tse.cargauy.dtos.FiltroInforme;
import tse.cargauy.dtos.InfraccionesPorTipoDto;
import tse.cargauy.dtos.InformeDto;
import tse.cargauy.dtos.NormaFiscalizacionDto;
import tse.cargauy.dtos.NotificacionDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.PistaAuditoriaDto;
import tse.cargauy.dtos.ViajesPorPeriodoDto;
import tse.cargauy.dtos.VolumenPorRubroDto;
import tse.cargauy.entities.DestinatarioNotificacion;
import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.entities.EstadoInforme;
import tse.cargauy.entities.FormatoInforme;
import tse.cargauy.entities.NormaFiscalizacion;
import tse.cargauy.entities.Rol;
import tse.cargauy.entities.TipoEvidencia;
import tse.cargauy.entities.TipoInfraccion;

// casos de fiscalizacion, notificaciones, normas, la pista de auditoria (R10) y los informes del backoffice, sobre los
// viajes y casos de la demo
class FiscalizacionDAOTest extends BaseDatos {

    private static final Paginacion TODO = new Paginacion(0, 100);
    private static final FiltroInforme ANIO = new FiltroInforme(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null);

    private CasoFiscalizacionDAO casos;
    private InformeDAO informes;
    private Long scc;

    @BeforeEach
    void preparar() {
        casos = bean(CasoFiscalizacionDAO.class);
        informes = bean(InformeDAO.class);
        scc = bean(VehiculoDAO.class).getVehiculoByMatricula("SCC9012").getId();
    }

    private CasoFiscalizacionDto nuevoCaso(String clave) {
        LocalDateTime hecho = LocalDateTime.of(2031, 3, 1, 10, 0);
        return casos.addCaso(new CasoFiscalizacionDto(TipoInfraccion.EXCESO_CARGA, clave, hecho, scc, null),
                List.of(new EvidenciaDto(TipoEvidencia.PESAJE, "balanzas/r5/" + clave, hecho, "36000 kg", "33800 kg", null, "pesada")),
                hecho.plusMinutes(5));
    }

    // la misma infraccion detectada dos veces se reconoce por su clave y no abre otro caso
    @Test
    void casoPorIdYPorClave() {
        CasoFiscalizacionDto caso = nuevoCaso("exceso|scc|prueba-1");
        sincronizar();

        assertEquals(caso.getId(), casos.getCasoById(caso.getId()).getId());
        assertEquals(caso.getId(), casos.getByClave("exceso|scc|prueba-1").getId());
        assertEquals(caso.getId(), casos.getIdPorClave("exceso|scc|prueba-1"));
        assertNull(casos.getByClave("no-existe"));
        assertNull(casos.getCasoById(-1L));
        assertEquals("SCC9012", casos.getCasoById(caso.getId()).getMatricula());
    }

    @Test
    void agregarEvidenciaAUnCasoAbierto() {
        CasoFiscalizacionDto caso = nuevoCaso("exceso|scc|prueba-2");
        LocalDateTime otra = LocalDateTime.of(2031, 3, 1, 14, 0);

        casos.agregarEvidencias(caso.getId(),
                List.of(new EvidenciaDto(TipoEvidencia.PESAJE, "balanzas/r1/otra", otra, "35500 kg", "33800 kg", null, "otra pesada")));
        sincronizar();

        assertEquals(2, casos.getCasoById(caso.getId()).getEvidencias().size());
    }

    @Test
    void buscarCasosPorEstadoYTipo() {
        PaginaDto<CasoFiscalizacionDto> confirmados = casos.buscar(new FiltroCasos(EstadoCaso.CONFIRMADA, null, null, null), TODO);
        PaginaDto<CasoFiscalizacionDto> desvios = casos.buscar(new FiltroCasos(null, TipoInfraccion.DESVIO_RUTA, null, null), TODO);

        assertTrue(confirmados.getTotal() >= 2);
        assertTrue(confirmados.getItems().stream().allMatch(c -> c.getEstado() == EstadoCaso.CONFIRMADA));
        assertTrue(desvios.getItems().stream().allMatch(c -> c.getTipo() == TipoInfraccion.DESVIO_RUTA));
        assertEquals(0, casos.buscar(new FiltroCasos(null, null, LocalDate.of(2040, 1, 1), null), TODO).getTotal());
    }

    // cada accion sobre el caso queda en la pista de auditoria, con quien la hizo y el cambio de estado
    @Test
    void pistaDeAuditoriaDeUnCasoResuelto() {
        Long resuelto = casos.buscar(new FiltroCasos(EstadoCaso.DESESTIMADA, null, null, null), TODO).getItems().get(0).getId();

        List<PistaAuditoriaDto> pista = casos.getAuditoria(resuelto);

        assertTrue(pista.size() >= 2, "deteccion, notificacion y resolucion");
        assertEquals(EstadoCaso.DESESTIMADA, pista.get(pista.size() - 1).getEstadoNuevo());
        assertNotNull(pista.get(pista.size() - 1).getFundamento());
    }

    // R10: la pista es de solo agregado; la base rechaza modificarla o borrarla aunque sea por SQL directo
    @Test
    void laPistaDeAuditoriaNoSeModificaNiSeBorra() {
        bean(InstaladorAuditoria.class).init();
        Long resuelto = casos.buscar(new FiltroCasos(EstadoCaso.CONFIRMADA, null, null, null), TODO).getItems().get(0).getId();
        assertFalse(casos.getAuditoria(resuelto).isEmpty());

        PersistenceException modificar = assertThrows(PersistenceException.class, () -> em
                .createNativeQuery("UPDATE pistaauditoria SET fundamento = 'cambiado' WHERE idcaso = " + resuelto)
                .executeUpdate());

        assertTrue(causas(modificar).contains("solo agregado"), causas(modificar));
    }

    @Test
    void laPistaDeAuditoriaNoSeBorra() {
        bean(InstaladorAuditoria.class).init();

        PersistenceException borrar = assertThrows(PersistenceException.class,
                () -> em.createNativeQuery("DELETE FROM pistaauditoria").executeUpdate());

        assertTrue(causas(borrar).contains("solo agregado"), causas(borrar));
    }

    private static String causas(Throwable e) {
        StringBuilder texto = new StringBuilder();
        for (Throwable t = e; t != null; t = t.getCause()) {
            texto.append(t.getMessage()).append(" | ");
        }
        return texto.toString();
    }

    @Test
    void notificacionesALaEmpresaYLeidas() {
        Long sur = bean(EmpresaDAO.class).getEmpresaByNro(1001).getId();
        Long empresaScc = bean(VehiculoDAO.class).getVehiculoById(scc).getIdEmpresa();

        PaginaDto<NotificacionDto> antes = casos.getNotificaciones(DestinatarioNotificacion.EMPRESA, List.of(empresaScc), TODO);
        assertTrue(antes.getTotal() > 0);
        assertTrue(antes.getItems().stream().allMatch(n -> n.getIdEmpresa().equals(empresaScc)));
        assertTrue(antes.getItems().stream().anyMatch(n -> !n.isLeida()));

        casos.marcarLeidas(DestinatarioNotificacion.EMPRESA, List.of(empresaScc));
        sincronizar();

        assertTrue(casos.getNotificaciones(DestinatarioNotificacion.EMPRESA, List.of(empresaScc), TODO).getItems().stream()
                .allMatch(NotificacionDto::isLeida));
        assertEquals(0, casos.getNotificaciones(DestinatarioNotificacion.EMPRESA, List.of(sur), TODO).getItems().stream()
                .filter(n -> n.getIdEmpresa().equals(empresaScc)).count(), "no ve las de otra empresa");
        assertNotNull(casos.getNotificaciones(DestinatarioNotificacion.FUNCIONARIO, null, TODO));
        casos.marcarLeidas(DestinatarioNotificacion.FUNCIONARIO, null);
    }

    // una norma nueva rige desde su fecha; antes sigue la anterior
    @Test
    void normasVigentesPorFecha() {
        String clave = NormaFiscalizacion.PLAZO_DESCARGO_DIAS;
        casos.addNorma(clave, 15, LocalDate.of(2030, 1, 1));
        sincronizar();

        assertEquals(10, casos.getNormaVigente(clave, LocalDate.of(2029, 12, 31)));
        assertEquals(15, casos.getNormaVigente(clave, LocalDate.of(2030, 1, 1)));
        assertEquals(15, casos.getNorma(clave, LocalDate.of(2030, 6, 1)).getValor());
        assertNull(casos.getNormaVigente(clave, LocalDate.of(2020, 1, 1)));
        PaginaDto<NormaFiscalizacionDto> historia = casos.getNormas(clave, TODO);
        assertEquals(2, historia.getTotal());
        assertEquals(15, historia.getItems().get(0).getValor(), "la mas nueva primero");
    }

    @Test
    void indicadoresDelInforme() {
        List<ViajesPorPeriodoDto> viajes = informes.contarViajesPorDia(ANIO);
        List<VolumenPorRubroDto> volumen = informes.sumarVolumenPorRubro(ANIO);
        List<InfraccionesPorTipoDto> infracciones = informes.contarInfracciones(ANIO);

        assertTrue(viajes.stream().mapToLong(ViajesPorPeriodoDto::getCantidad).sum() >= 15, "los viajes realizados de la demo");
        assertTrue(volumen.stream().allMatch(v -> v.getVolumen() > 0));
        assertTrue(infracciones.stream().anyMatch(i -> i.getTipo() == TipoInfraccion.EXCESO_CARGA && i.getConfirmadas() >= 1));
        assertTrue(informes.contarVehiculosHabilitados(LocalDate.of(2026, 10, 15)) >= 3);
        assertTrue(informes.contarViajesPorDia(new FiltroInforme(LocalDate.of(2040, 1, 1), LocalDate.of(2040, 12, 31), null)).isEmpty());

        Long tipo = viajes.isEmpty() ? null : bean(tse.cargauy.data.guia.GuiaDAO.class).getTiposCarga().get(0).getId();
        assertNotNull(informes.contarViajesPorDia(new FiltroInforme(ANIO.getDesde(), ANIO.getHasta(), tipo)));
    }

    // un informe pedido queda pendiente, se genera en segundo plano y vence pasado el plazo
    @Test
    void cicloDeVidaDeUnInforme() {
        Long autoridad = bean(UsuarioDAO.class).saveUsuarioBackoffice("autoridad", List.of(Rol.AUTORIDAD)).getId();
        InformeDto pedido = new InformeDto();
        pedido.setPeriodoDesde(ANIO.getDesde());
        pedido.setPeriodoHasta(ANIO.getHasta());
        pedido.setFormato(FormatoInforme.CSV);
        pedido.setEstado(EstadoInforme.PENDIENTE);
        pedido.setFechaSolicitud(LocalDateTime.of(2026, 11, 1, 9, 0));
        pedido.setIdSolicitante(autoridad);
        InformeDto informe = informes.addInforme(pedido);
        InformeDto otro = informes.addInforme(pedido);
        sincronizar();
        assertTrue(informes.tienePendientes(autoridad));

        byte[] csv = "fecha;viajes\n".getBytes();
        informes.guardarContenido(informe.getId(), csv, LocalDateTime.of(2026, 11, 1, 9, 5));
        informes.marcarFallido(otro.getId());
        sincronizar();
        assertEquals(EstadoInforme.GENERADO, informes.getInformeById(informe.getId()).getEstado());
        assertArrayEquals(csv, informes.getContenido(informe.getId()));
        assertEquals(EstadoInforme.FALLIDO, informes.getInformeById(otro.getId()).getEstado());
        assertFalse(informes.tienePendientes(autoridad));
        assertEquals(2, informes.getBySolicitante(autoridad, TODO).getTotal());

        assertEquals(1, informes.vencer(LocalDateTime.of(2026, 11, 2, 0, 0)));
        sincronizar();
        assertEquals(EstadoInforme.VENCIDO, informes.getInformeById(informe.getId()).getEstado());
        assertNull(informes.getContenido(informe.getId()), "vencido ya no tiene el archivo");
        assertNull(informes.getInformeById(-1L));
    }
}
