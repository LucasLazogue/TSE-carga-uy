package tse.cargauy.negocio.empresa;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

class EmpresaEJBTest {

    private final Map<String, Long> ciudadanos = new HashMap<>(Map.of("22222222", 5L));
    private final List<EmpresaDto> empresas = new ArrayList<>();
    private final List<List<Object>> responsables = new ArrayList<>();
    private final EmpresaEJB ejb = new EmpresaEJB();

    @BeforeEach
    void iniciar() {
        ejb.empresaDAO = new EmpresaDAO() {
            @Override
            public EmpresaDto getEmpresaByNro(int nroEmpresa) {
                return empresas.stream().filter(e -> e.getNroEmpresa() == nroEmpresa).findFirst().orElse(null);
            }

            @Override
            public EmpresaDto addEmpresa(EmpresaDto empresaDto) {
                empresaDto.setId((long) empresas.size() + 1);
                empresas.add(empresaDto);
                return empresaDto;
            }
        };
        ejb.usuarioDAO = new UsuarioDAO() {
            @Override
            public UsuarioDto getFuncionarioByCedula(String cedula) {
                return "11111111".equals(cedula) ? new UsuarioDto() : null;
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
            public void addResponsable(Long idCiudadano, Long idEmpresa, LocalDate fechaDesde) {
                responsables.add(List.of(idCiudadano, idEmpresa, fechaDesde));
            }
        };
    }

    private static UsuarioDto usuario(Long id) {
        UsuarioDto usuario = new UsuarioDto();
        usuario.setId(id);
        return usuario;
    }

    private static EmpresaDto empresa(String cedulaResponsable) {
        EmpresaDto empresa = new EmpresaDto(1001, "Transportes del Sur", "Transportes del Sur S.A.", "Av. Italia 3456", null);
        empresa.setCedulaResponsable(cedulaResponsable);
        return empresa;
    }

    private static void assertCodigo(CodigoError esperado, Executable accion) {
        assertEquals(esperado, assertThrows(CargaUYException.class, accion).getCodigo());
    }

    @Test
    void altaConResponsableQueYaIngreso() {
        EmpresaDto creada = ejb.addEmpresa(empresa("22222222"));

        assertEquals(List.of(List.of(5L, creada.getId(), LocalDate.now())), responsables);
        assertEquals(1, ciudadanos.size());
    }

    @Test
    void altaConResponsableQueNuncaIngresoLoCreaSoloConLaCedula() {
        EmpresaDto creada = ejb.addEmpresa(empresa("12345672"));

        Long id = ciudadanos.get("12345672");
        assertEquals(List.of(List.of(id, creada.getId(), LocalDate.now())), responsables);
    }

    @Test
    void altaSinResponsableValido() {
        assertCodigo(CodigoError.EMPRESA_RESPONSABLE_REQUERIDO, () -> ejb.addEmpresa(empresa(" ")));
        assertCodigo(CodigoError.CEDULA_INVALIDA, () -> ejb.addEmpresa(empresa("12345678")));
        assertCodigo(CodigoError.EMPRESA_RESPONSABLE_FUNCIONARIO, () -> ejb.addEmpresa(empresa("11111111")));
        assertTrue(empresas.isEmpty());
        assertTrue(responsables.isEmpty());
    }
}
