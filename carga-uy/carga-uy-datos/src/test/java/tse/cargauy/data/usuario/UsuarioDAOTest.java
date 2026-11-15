package tse.cargauy.data.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import tse.cargauy.entities.Ciudadano;

class UsuarioDAOTest {

    private final UsuarioDAO dao = new UsuarioDAO();
    private final Ciudadano ciudadano = new Ciudadano("22222222", "ana@correo.uy");

    // sin base: find solo encuentra al ciudadano 7 y el test mira como queda la entidad
    @BeforeEach
    void preparar() throws Exception {
        EntityManager entityManager = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { EntityManager.class }, (proxy, metodo, args) ->
                        metodo.getName().equals("find") && args[0] == Ciudadano.class && args[1].equals(7L)
                                ? ciudadano
                                : null);
        Field campo = UsuarioDAO.class.getDeclaredField("entityManager");
        campo.setAccessible(true);
        campo.set(dao, entityManager);
    }

    // son tres String seguidos: el compilador no avisa si se cruzan
    @Test
    void guardaCadaDatoDeLaPdiEnSuCampo() {
        dao.updateDatosPdi(7L, "Ana María", "Pereira Gómez", "1975-11-03");

        assertEquals("Ana María", ciudadano.getNombres());
        assertEquals("Pereira Gómez", ciudadano.getApellidos());
        assertEquals("1975-11-03", ciudadano.getFechaNacimiento());
    }
}
