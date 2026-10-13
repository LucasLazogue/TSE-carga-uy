package tse.cargauy.data.fiscalizacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import tse.cargauy.dtos.CasoFiscalizacionDto;
import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.entities.TipoInfraccion;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

class CasoFiscalizacionDAOTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 13, 10, 0);

    private final CasoFiscalizacionDAO dao = new CasoFiscalizacionDAO();
    private RuntimeException alHacerFlush;

    // sin base: find devuelve entidades vacias y el flush falla con lo que diga cada test
    @BeforeEach
    void preparar() throws Exception {
        EntityManager entityManager = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { EntityManager.class }, (proxy, metodo, args) -> {
                    if (metodo.getName().equals("find")) {
                        return ((Class<?>) args[0]).getConstructor().newInstance();
                    }
                    if (metodo.getName().equals("flush") && alHacerFlush != null) {
                        throw alHacerFlush;
                    }
                    return null;
                });
        Field campo = CasoFiscalizacionDAO.class.getDeclaredField("entityManager");
        campo.setAccessible(true);
        campo.set(dao, entityManager);
    }

    private static PersistenceException errorDeBase(String estadoSql) {
        return new PersistenceException(new RuntimeException(new SQLException("error", estadoSql)));
    }

    private static void rechaza(CodigoError codigo, Executable accion) {
        assertEquals(codigo, assertThrows(CargaUYException.class, accion).getCodigo());
    }

    @Test
    void laResolucionSobreUnaVersionVieja() {
        alHacerFlush = new OptimisticLockException();

        rechaza(CodigoError.CASO_MODIFICADO_CONCURRENTEMENTE, () -> dao.resolver(1L, 2L, EstadoCaso.CONFIRMADA, "fundamento", AHORA));
    }

    @Test
    void dosDescargosEnParaleloChocanEnLaClaveUnica() {
        alHacerFlush = errorDeBase("23505");

        rechaza(CodigoError.CASO_MODIFICADO_CONCURRENTEMENTE, () -> dao.presentarDescargo(1L, 2L, "texto", AHORA));
    }

    @Test
    void elMismoHechoRegistradoEnParaleloYaExiste() {
        alHacerFlush = errorDeBase("23505");
        CasoFiscalizacionDto hecho = new CasoFiscalizacionDto(TipoInfraccion.EXCESO_CARGA, "h1", AHORA, 1L, null);

        rechaza(CodigoError.CASO_YA_REGISTRADO, () -> dao.addCaso(hecho, List.of(), AHORA));
    }

    @Test
    void otrosErroresDeLaBaseSePropagan() {
        PersistenceException error = errorDeBase("23502");
        alHacerFlush = error;

        assertSame(error, assertThrows(PersistenceException.class,
                () -> dao.resolver(1L, 2L, EstadoCaso.CONFIRMADA, "fundamento", AHORA)));
    }
}
