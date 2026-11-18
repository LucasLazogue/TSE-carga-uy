package tse.cargauy.web.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.omnifaces.cdi.ViewScoped;

import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.dtos.NormaFiscalizacionDto;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.entities.NormaFiscalizacion;
import tse.cargauy.entities.TipoNodo;
import tse.cargauy.negocio.empresa.EmpresaEJBLocal;
import tse.cargauy.negocio.fiscalizacion.NormaFiscalizacionEJBLocal;
import tse.cargauy.negocio.nodo.NodoPerifericoEJBLocal;
import tse.cargauy.negocio.usuario.FuncionarioEJBLocal;
import tse.cargauy.negocio.usuario.UsuarioBackofficeEJBLocal;
import tse.cargauy.negocio.vehiculo.VehiculoEJBLocal;

// los beans de las pantallas viajan serializados en el estado de vista (en la pagina): lo que tienen tiene que
// sobrevivir a la serializacion, porque el clic siguiente lo puede atender otra instancia
class VistasEnLaPaginaTest {

    @AfterEach
    void soltar() {
        ContextoFaces.soltar();
    }

    @SuppressWarnings("unchecked")
    private static <T> T idaYVuelta(T bean) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream salida = new ObjectOutputStream(bytes)) {
            salida.writeObject(bean);
        }
        try (ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) entrada.readObject();
        }
    }

    private static void inyectar(Object bean, String campo, Object valor) throws ReflectiveOperationException {
        Field f = bean.getClass().getDeclaredField(campo);
        f.setAccessible(true);
        f.set(bean, valor);
    }

    private static <T> T ejb(Class<T> tipo) {
        // en wildfly el proxy de un EJB local se serializa; aca lo simula un mock serializable
        return mock(tipo, withSettings().serializable());
    }

    @Test
    void lasCincoPantallasGuardanElBeanEnLaPagina() {
        for (Class<?> bean : List.of(EmpresaView.class, NodoView.class, NormaView.class, UsuarioView.class,
                VehiculoView.class)) {
            ViewScoped alcance = bean.getAnnotation(ViewScoped.class);
            assertNotNull(alcance, bean.getSimpleName());
            assertTrue(alcance.saveInViewState(), bean.getSimpleName());
            assertTrue(Serializable.class.isAssignableFrom(bean), bean.getSimpleName());
        }
    }

    @Test
    void normaConservaLaEdicionYElHistorial() throws Exception {
        NormaView vista = new NormaView();
        inyectar(vista, "normaEJB", new NormasFalsas());
        vista.init();
        NormaFiscalizacionDto norma = vista.getVigentes().get(0);
        vista.editar(norma);
        vista.setValor(9);

        NormaView otra = idaYVuelta(vista);

        assertEquals(NormaFiscalizacion.TOLERANCIA_EXCESO_CARGA, otra.getClave());
        assertEquals(9, otra.getValor());
        assertEquals(LocalDate.now(), otra.getVigenteDesde());
        assertEquals(1, otra.getVigentes().size());
        // el historial paginado consulta con la clave elegida tambien despues de viajar
        assertEquals(3, otra.getHistorial().count(Map.of()));
    }

    @Test
    void usuarioConservaLaEdicionPeroNoLaClave() throws Exception {
        ContextoFaces.usar(rol -> true);
        UsuarioView vista = new UsuarioView();
        UsuarioBackofficeEJBLocal usuarios = ejb(UsuarioBackofficeEJBLocal.class);
        inyectar(vista, "usuarioBackofficeEJB", usuarios);
        inyectar(vista, "funcionarioEJB", ejb(FuncionarioEJBLocal.class));
        vista.editar(new UsuarioDto("ana", new ArrayList<>(List.of("AUTORIDAD"))));
        vista.setClave("clave-nueva");

        UsuarioView otra = idaYVuelta(vista);
        assertTrue(otra.isEdicion());
        assertEquals("ana", otra.getUsuario().getUsuario());
        assertEquals(List.of("AUTORIDAD"), otra.getUsuario().getRoles());

        vista.guardar();
        verify(usuarios).updateUsuario(vista.getUsuario(), "clave-nueva");
        assertNull(vista.getClave());
    }

    @Test
    void usuarioAltaNoDejaLaClave() throws Exception {
        ContextoFaces.usar(rol -> true);
        UsuarioView vista = new UsuarioView();
        UsuarioBackofficeEJBLocal usuarios = ejb(UsuarioBackofficeEJBLocal.class);
        inyectar(vista, "usuarioBackofficeEJB", usuarios);
        vista.nuevo();
        vista.getUsuario().setUsuario("beto");
        vista.setClave("clave-inicial");
        vista.guardar();
        verify(usuarios).addUsuario(vista.getUsuario(), "clave-inicial");
        assertNull(vista.getClave());
    }

    @Test
    void nodoConservaElRegistroYDespuesDeGuardarNoDejaLaClave() throws Exception {
        ContextoFaces.usar(rol -> true);
        NodoView vista = new NodoView();
        NodoPerifericoEJBLocal nodos = ejb(NodoPerifericoEJBLocal.class);
        inyectar(vista, "nodoEJB", nodos);
        vista.nuevo();
        vista.getNodo().setIdentificador("balanzas-sur");
        vista.getNodo().setTipo(TipoNodo.BALANZAS);
        vista.getNodo().setClave("clave-del-nodo");

        NodoView otra = idaYVuelta(vista);
        assertEquals("balanzas-sur", otra.getNodo().getIdentificador());
        assertEquals(TipoNodo.BALANZAS, otra.getNodo().getTipo());

        NodoPerifericoDto registrado = vista.getNodo();
        vista.createNodo();
        verify(nodos).addNodo(registrado);
        assertNull(vista.getNodo().getClave());
        assertNull(vista.getNodo().getIdentificador());
    }

    @Test
    void empresaYVehiculoConservanElFormulario() throws Exception {
        EmpresaView empresas = new EmpresaView();
        EmpresaEJBLocal empresaEJB = ejb(EmpresaEJBLocal.class);
        inyectar(empresas, "empresaEJB", empresaEJB);
        empresas.setNombre("Cargas");
        empresas.getEmpresa().setRazonSocial("Cargas del Norte S.R.L.");
        EmpresaView otraEmpresa = idaYVuelta(empresas);
        assertEquals("Cargas", otraEmpresa.getNombre());
        assertEquals("Cargas del Norte S.R.L.", otraEmpresa.getEmpresa().getRazonSocial());

        VehiculoView vehiculos = new VehiculoView();
        inyectar(vehiculos, "vehiculoEJB", ejb(VehiculoEJBLocal.class));
        inyectar(vehiculos, "empresaEJB", empresaEJB);
        vehiculos.setIdEmpresa(2L);
        vehiculos.setVehiculo(new VehiculoDto());
        vehiculos.getVehiculo().setMatricula("SCC9012");
        VehiculoView otroVehiculo = idaYVuelta(vehiculos);
        assertEquals(2L, otroVehiculo.getIdEmpresa());
        assertEquals("SCC9012", otroVehiculo.getVehiculo().getMatricula());
    }

    // EJB de normas serializable, con datos fijos
    static class NormasFalsas implements NormaFiscalizacionEJBLocal, Serializable {

        private static final long serialVersionUID = 1L;

        @Override
        public List<NormaFiscalizacionDto> getVigentes() {
            NormaFiscalizacionDto norma = new NormaFiscalizacionDto();
            norma.setClave(NormaFiscalizacion.TOLERANCIA_EXCESO_CARGA);
            norma.setValor(5);
            return List.of(norma);
        }

        @Override
        public PaginaDto<NormaFiscalizacionDto> getHistorial(String clave, Paginacion paginacion) {
            assertEquals(NormaFiscalizacion.TOLERANCIA_EXCESO_CARGA, clave);
            return new PaginaDto<>(List.of(), 3, paginacion.getPagina(), paginacion.getTamanio());
        }

        @Override
        public void cambiarValor(String clave, int valor, LocalDate vigenteDesde) {
            // no hace falta en estas pruebas
        }
    }
}
