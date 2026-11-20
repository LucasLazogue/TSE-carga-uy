package tse.cargauy.data.fiscalizacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.JoinType;
import tse.cargauy.data.CriteriaGrabada;
import tse.cargauy.dtos.CasoFiscalizacionDto;
import tse.cargauy.dtos.FiltroCasos;
import tse.cargauy.dtos.PaginaDto;
import tse.cargauy.dtos.Paginacion;
import tse.cargauy.entities.CasoFiscalizacion;
import tse.cargauy.entities.DestinatarioNotificacion;
import tse.cargauy.entities.Empresa;
import tse.cargauy.entities.EstadoCaso;
import tse.cargauy.entities.Notificacion;
import tse.cargauy.entities.TipoInfraccion;
import tse.cargauy.entities.Vehiculo;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.exceptions.CodigoError;

class CasoFiscalizacionDAOTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 13, 10, 0);

    private final CasoFiscalizacionDAO dao = new CasoFiscalizacionDAO();
    private RuntimeException alHacerFlush;
    private Notificacion notificacion;
    private List<?> resultado = List.of();
    private Object claveConsultada;

    // sin base: find devuelve entidades vacias (salvo el caso, con su vehiculo y empresa), persist guarda la
    // notificacion y el flush falla con lo que diga cada test
    @BeforeEach
    void preparar() throws Exception {
        EntityManager entityManager = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { EntityManager.class }, (proxy, metodo, args) -> {
                    if (metodo.getName().equals("createQuery")) {
                        return consulta();
                    }
                    if (metodo.getName().equals("find")) {
                        return args[0] == CasoFiscalizacion.class ? caso() : ((Class<?>) args[0]).getConstructor().newInstance();
                    }
                    if (metodo.getName().equals("persist") && args[0] instanceof Notificacion n) {
                        notificacion = n;
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

    // la consulta devuelve 'resultado' y guarda el parametro clave
    private TypedQuery<?> consulta() {
        return (TypedQuery<?>) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { TypedQuery.class },
                (proxy, metodo, args) -> {
                    if (metodo.getName().equals("setParameter") && "clave".equals(args[0])) {
                        claveConsultada = args[1];
                    }
                    return metodo.getName().equals("getResultList") ? resultado : proxy;
                });
    }

    private static CasoFiscalizacion caso() {
        Empresa empresa = new Empresa(1, "Transportes Sur", "Transportes Sur SA", "Ruta 5 km 30", LocalDate.of(2020, 1, 1));
        Vehiculo vehiculo = new Vehiculo("SBA1234", "Volvo", "FH", 9000, 30000, empresa);
        CasoFiscalizacion caso = new CasoFiscalizacion(TipoInfraccion.EXCESO_CARGA, "h1", AHORA, AHORA, vehiculo, null);
        caso.setId(7L);
        return caso;
    }

    private static PersistenceException errorDeBase(String estadoSql) {
        return new PersistenceException(new RuntimeException(new SQLException("error", estadoSql)));
    }

    private static void rechaza(CodigoError codigo, Executable accion) {
        assertEquals(codigo, assertThrows(CargaUYException.class, accion).getCodigo());
    }

    @Test
    void elIdDeUnCasoYaRegistradoPorSuClave() {
        resultado = List.of(7L);

        assertEquals(7L, dao.getIdPorClave("SIN_HABILITACION|11|2026-10-13"));
        assertEquals("SIN_HABILITACION|11|2026-10-13", claveConsultada);
    }

    @Test
    void sinCasoConEsaClaveNoHayId() {
        assertNull(dao.getIdPorClave("SIN_HABILITACION|11|2026-10-13"));
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

    @Test
    void elDescargoAvisaALosFuncionarios() {
        dao.presentarDescargo(7L, 2L, "texto", AHORA);

        assertEquals(DestinatarioNotificacion.FUNCIONARIO, notificacion.getDestinatario());
        assertEquals("Transportes Sur presento un descargo en el caso 7 (Exceso de carga, vehiculo SBA1234).", notificacion.getMensaje());
    }

    @Test
    void laResolucionAvisaALaEmpresa() {
        dao.resolver(7L, 2L, EstadoCaso.DESESTIMADA, "fundamento", AHORA);

        assertEquals(DestinatarioNotificacion.EMPRESA, notificacion.getDestinatario());
        assertEquals("El caso 7 (Exceso de carga, vehiculo SBA1234) fue desestimado.", notificacion.getMensaje());
    }

    // la pagina de casos trae el descargo y la resolucion en la misma consulta (son @OneToOne del lado mappedBy: si no,
    // se buscan de a uno por caso) y se lee como lista, para que el resto de las relaciones se carguen por lotes
    @Test
    void laPaginaDeCasosTraeDescargoYResolucionEnLaMismaConsulta() throws Exception {
        CriteriaGrabada criteria = new CriteriaGrabada();
        criteria.total = 3;
        CasoFiscalizacionDAO otro = new CasoFiscalizacionDAO();
        Field campo = CasoFiscalizacionDAO.class.getDeclaredField("entityManager");
        campo.setAccessible(true);
        campo.set(otro, criteria.entityManager());

        PaginaDto<CasoFiscalizacionDto> pagina = otro.buscar(new FiltroCasos(), new Paginacion(0, 20));

        assertEquals(3, pagina.getTotal());
        assertEquals(2, criteria.de("fetch").size());
        assertTrue(criteria.de("fetch").stream().allMatch(l -> l.argumentos().get(1) == JoinType.LEFT));
        assertEquals(1, criteria.de("getResultList").size());
        assertTrue(criteria.de("getResultStream").isEmpty());
    }
}
