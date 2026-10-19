package tse.cargauy.negocio.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import tse.cargauy.data.usuario.UsuarioDAO;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

class FuncionarioEJBTest {

    private final Set<String> cedulas = new HashSet<>(Set.of("11111111"));
    private final FuncionarioEJB ejb = new FuncionarioEJB();

    @BeforeEach
    void iniciar() {
        ejb.usuarioDAO = new UsuarioDAO() {
            @Override
            public UsuarioDto getFuncionarioByCedula(String cedula) {
                return cedulas.contains(cedula) ? new UsuarioDto() : null;
            }

            @Override
            public void addFuncionario(String cedula) {
                cedulas.add(cedula);
            }

            @Override
            public void deleteFuncionario(String cedula) {
                cedulas.remove(cedula);
            }
        };
    }

    private static void assertCodigo(CodigoError esperado, Executable accion) {
        assertEquals(esperado, assertThrows(CargaUYException.class, accion).getCodigo());
    }

    @Test
    void altaYBaja() {
        ejb.addFuncionario("22222222");
        ejb.deleteFuncionario("11111111");

        assertEquals(Set.of("22222222"), cedulas);
    }

    @Test
    void altaSinCedulaODuplicada() {
        assertCodigo(CodigoError.FUNCIONARIO_CEDULA_REQUERIDA, () -> ejb.addFuncionario(" "));
        assertCodigo(CodigoError.FUNCIONARIO_DUPLICADO, () -> ejb.addFuncionario("11111111"));
        assertEquals(Set.of("11111111"), cedulas);
    }

    @Test
    void bajaInexistente() {
        assertCodigo(CodigoError.FUNCIONARIO_NO_ENCONTRADO, () -> ejb.deleteFuncionario("99999999"));
    }
}
