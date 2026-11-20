package tse.cargauy.data.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tse.cargauy.data.BaseDatos;
import tse.cargauy.data.empresa.EmpresaDAO;
import tse.cargauy.dtos.ChoferDto;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.FiltroChoferes;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.entities.Rol;

// usuarios, choferes y responsables, y las empresas que ve cada responsable. En la demo Carlos (55555555) es chofer y
// responsable de Transportes del Sur (1001) y Ana (22222222) responsable de Cargas Norte (1002)
class PersonasDAOTest extends BaseDatos {

    private static final Paginacion TODO = new Paginacion(0, 100);
    private static final LocalDate HOY = LocalDate.of(2026, 11, 1);

    private UsuarioDAO usuarios;
    private EmpresaDAO empresas;
    private Long sur;
    private Long carlos;

    @BeforeEach
    void preparar() {
        usuarios = bean(UsuarioDAO.class);
        empresas = bean(EmpresaDAO.class);
        sur = empresas.getEmpresaByNro(1001).getId();
        carlos = usuarios.getCiudadanoByCedula("55555555").getId();
    }

    @Test
    void ciudadanoPorIdYCambioDeCorreo() {
        usuarios.updateCorreo(carlos, "carlos@nuevo.uy");
        sincronizar();

        UsuarioDto usuario = usuarios.getById(carlos);
        assertEquals("55555555", usuario.getCedula());
        assertEquals("carlos@nuevo.uy", usuario.getCorreo());
        assertNull(usuarios.getById(-1L));
    }

    @Test
    void funcionarios() {
        usuarios.addFuncionario("33333333");
        sincronizar();

        PaginaDto<UsuarioDto> pagina = usuarios.getFuncionarios(TODO);
        assertTrue(pagina.getItems().stream().anyMatch(f -> "33333333".equals(f.getCedula())));
        assertEquals("11111111", pagina.getItems().get(0).getCedula(), "ordenados por cedula");

        usuarios.deleteFuncionario("33333333");
        usuarios.deleteFuncionario("no-existe");
        sincronizar();
        assertNull(usuarios.getFuncionarioByCedula("33333333"));
    }

    // los usuarios del backoffice vienen de LDAP: se crean al ingresar y se les actualizan los roles
    @Test
    void usuariosDelBackofficeConSusRoles() {
        UsuarioDto creado = usuarios.saveUsuarioBackoffice("admin", List.of(Rol.ADMINISTRADOR));
        assertEquals(List.of(Rol.ADMINISTRADOR), creado.getRoles());

        UsuarioDto actualizado = usuarios.saveUsuarioBackoffice("admin", List.of(Rol.AUTORIDAD, Rol.FUNCIONARIO));
        sincronizar();
        assertEquals(creado.getId(), actualizado.getId());
        assertEquals(2, usuarios.getById(creado.getId()).getRoles().size());
        assertTrue(usuarios.getById(creado.getId()).getRoles().contains(Rol.AUTORIDAD));

        usuarios.deleteUsuarioBackoffice("admin");
        usuarios.deleteUsuarioBackoffice("no-existe");
        sincronizar();
        assertNull(usuarios.getById(creado.getId()));
    }

    @Test
    void altaListadoYBajaDeUnChofer() {
        Long pedro = usuarios.addCiudadano("44444444", "pedro@example.com").getId();
        ChoferDto vinculo = usuarios.addChofer(pedro, sur, LocalDate.of(2026, 5, 1));
        sincronizar();

        PaginaDto<ChoferDto> vigentes = usuarios.getChoferes(sur, new FiltroChoferes(null, HOY, false), TODO);
        assertTrue(vigentes.getItems().stream().anyMatch(c -> "44444444".equals(c.getCedula())));
        PaginaDto<ChoferDto> porCedula = usuarios.getChoferes(sur, new FiltroChoferes("4444", HOY, false), TODO);
        assertEquals(1, porCedula.getTotal());
        assertEquals(0, usuarios.getChoferes(sur, new FiltroChoferes(null, LocalDate.of(2026, 4, 1), false), TODO)
                .getItems().stream().filter(c -> "44444444".equals(c.getCedula())).count(), "todavia no habia entrado");

        Long idVinculo = vinculo.getIdVinculo() != null ? vinculo.getIdVinculo() : vinculo.getId();
        assertNotNull(usuarios.getVinculoSinBaja(idVinculo, sur));
        assertNull(usuarios.getVinculoSinBaja(idVinculo, empresas.getEmpresaByNro(1002).getId()), "es de otra empresa");

        ChoferDto baja = usuarios.darDeBajaChofer(idVinculo, HOY);
        sincronizar();
        assertEquals(HOY, baja.getFechaHasta());
        assertNull(usuarios.getVinculoSinBaja(idVinculo, sur), "ya tiene baja");
        assertNull(usuarios.getChoferSinBaja(pedro, sur, HOY));
        assertTrue(usuarios.getChoferes(sur, new FiltroChoferes(null, HOY, false), TODO).getItems().stream()
                .noneMatch(c -> "44444444".equals(c.getCedula())));
        assertTrue(usuarios.getChoferes(sur, new FiltroChoferes(null, null, true), TODO).getItems().stream()
                .anyMatch(c -> "44444444".equals(c.getCedula())), "el historial incluye las bajas");
    }

    @Test
    void empresasDeUnResponsable() {
        List<EmpresaDto> deCarlos = empresas.getByResponsable(carlos, HOY);
        PaginaDto<EmpresaDto> paginadas = empresas.getByResponsable(carlos, HOY, TODO);

        assertTrue(deCarlos.stream().anyMatch(e -> e.getNroEmpresa() == 1001));
        assertTrue(deCarlos.stream().noneMatch(e -> e.getNroEmpresa() == 1002));
        assertEquals(deCarlos.size(), paginadas.getTotal());
        assertTrue(empresas.getByResponsable(carlos, LocalDate.of(2020, 1, 1)).isEmpty(), "era responsable desde 2024");
    }

    @Test
    void buscarModificarYBorrarEmpresas() {
        assertTrue(empresas.findByNombre("norte", TODO).getItems().stream().anyMatch(e -> e.getNroEmpresa() == 1002));
        assertTrue(empresas.findByNombre("S.R.L.", TODO).getItems().stream().anyMatch(e -> e.getNroEmpresa() == 1002),
                "tambien busca por razon social");
        assertEquals(0, empresas.findByNombre("no existe ninguna", TODO).getTotal());
        assertEquals(1, empresas.getByRazonSocial("  logistica del este ltda. ", 5).size());

        EmpresaDto nueva = empresas.addEmpresa(new EmpresaDto(9001, "Prueba", "Prueba S.A.", "Calle 1", HOY));
        empresas.updateEmpresa(nueva.getId(), new EmpresaDto(9002, "Prueba 2", "Prueba 2 S.A.", "Calle 2", HOY));
        empresas.updateEmpresa(-1L, nueva);
        sincronizar();
        EmpresaDto modificada = empresas.getEmpresaById(nueva.getId());
        assertEquals(9002, modificada.getNroEmpresa());
        assertEquals("Calle 2", modificada.getDireccionPrincipal());

        empresas.deleteEmpresa(nueva.getId());
        empresas.deleteEmpresa(-1L);
        sincronizar();
        assertNull(empresas.getEmpresaById(nueva.getId()));
    }
}
