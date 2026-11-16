package tse.cargauy.negocio.demo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tse.cargauy.data.CerrojoArranque;
import tse.cargauy.data.empresa.EmpresaDAOLocal;
import tse.cargauy.data.guia.GuiaDAOLocal;
import tse.cargauy.data.nodo.NodoPerifericoDAOLocal;
import tse.cargauy.data.testdata.DatosPrueba;
import tse.cargauy.data.usuario.UsuarioDAOLocal;
import tse.cargauy.data.vehiculo.VehiculoDAOLocal;
import tse.cargauy.data.viaje.ViajeDAOLocal;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.GuiaDto;
import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.dtos.UsuarioDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.dtos.ViajeDto;
import tse.cargauy.entities.EstadoNodo;
import tse.cargauy.entities.TipoNodo;
import tse.cargauy.seguridad.Cifrado;

// el elenco de la demo: quien es funcionario, responsable o chofer, y los nodos perifericos ya registrados y
// habilitados. Las cedulas son de personas reales (cuentas de gub.uy de testing) y las claves de los nodos son
// secretas, asi que no van en el repositorio: se leen del entorno (un Secret en OpenShift, el .env en Compose).
// Sin esas variables no hace nada. Corre despues de los datos de prueba, sobre los que se apoya, y solo agrega lo
// que falta: arrancar de nuevo no duplica nada
@Singleton
@Startup
public class ElencoDemo {

    private static final Logger LOGGER = Logger.getLogger(ElencoDemo.class.getName());

    static final int EMPRESA_POR_DEFECTO = 1001;
    // lo reporta el nodo tracking-1 (flotas/flota-sur.json): el viaje de respaldo tiene posiciones apenas se inicia
    static final String VEHICULO_RESPALDO = "SBB5678";
    private static final LocalDate DESDE = LocalDate.of(2024, 1, 1);

    @PersistenceContext(unitName = "carga-uy")
    EntityManager entityManager;

    // los datos de prueba (empresas, vehiculos, rubros) tienen que estar antes: llamarlo los crea si faltan y si ya
    // estan no hace nada
    @EJB
    DatosPrueba datosPrueba;

    @EJB
    UsuarioDAOLocal usuarioDAO;

    @EJB
    EmpresaDAOLocal empresaDAO;

    @EJB
    VehiculoDAOLocal vehiculoDAO;

    @EJB
    GuiaDAOLocal guiaDAO;

    @EJB
    ViajeDAOLocal viajeDAO;

    @EJB
    NodoPerifericoDAOLocal nodoDAO;

    @Inject
    Cifrado cifrado;

    @PostConstruct
    void iniciar() {
        List<String> funcionarios = cedulas(entorno("DEMO_FUNCIONARIOS"));
        List<String> responsables = cedulas(entorno("DEMO_RESPONSABLES"));
        List<String> choferes = cedulas(entorno("DEMO_CHOFERES"));
        List<String> ciudadanos = cedulas(entorno("DEMO_CIUDADANOS"));
        List<NodoDemo> nodos = nodos(entorno("DEMO_NODOS"));
        if (funcionarios.isEmpty() && responsables.isEmpty() && choferes.isEmpty() && ciudadanos.isEmpty() && nodos.isEmpty()) {
            return;
        }
        // primero los datos de prueba y despues el cerrojo, en ese orden: DatosPrueba tambien arranca solo y toma el
        // cerrojo. Si este bean lo tomara antes, la llamada esperaria a que termine el arranque de DatosPrueba, que a
        // su vez espera el cerrojo: quedaban trabados hasta el timeout de la transaccion y el servidor no arrancaba
        datosPrueba.init();
        CerrojoArranque.tomar(entityManager);

        EmpresaDto empresa = empresaDAO.getEmpresaByNro(numero(entorno("DEMO_EMPRESA"), EMPRESA_POR_DEFECTO));
        for (String cedula : funcionarios) {
            if (usuarioDAO.getFuncionarioByCedula(cedula) == null) {
                usuarioDAO.addFuncionario(cedula);
            }
        }
        List<UsuarioDto> responsablesDemo = new ArrayList<>();
        for (String cedula : responsables) {
            UsuarioDto ciudadano = ciudadano(cedula);
            if (empresa != null && usuarioDAO.getResponsableVigente(ciudadano.getId(), empresa.getId(), LocalDate.now()) == null) {
                usuarioDAO.addResponsable(ciudadano.getId(), empresa.getId(), DESDE);
            }
            responsablesDemo.add(ciudadano);
        }
        for (String cedula : choferes) {
            UsuarioDto ciudadano = ciudadano(cedula);
            if (empresa != null && usuarioDAO.getChoferVigente(ciudadano.getId(), empresa.getId(), LocalDate.now()) == null) {
                usuarioDAO.addChofer(ciudadano.getId(), empresa.getId(), DESDE);
            }
            if (empresa != null && !responsablesDemo.isEmpty()) {
                viajeDeRespaldo(empresa, responsablesDemo.get(0), ciudadano);
            }
        }
        for (String cedula : ciudadanos) {
            ciudadano(cedula);
        }
        for (NodoDemo nodo : nodos) {
            registrar(nodo);
        }
        LOGGER.info("demo elenco=CARGADO funcionarios=" + funcionarios.size() + " responsables=" + responsables.size()
                + " choferes=" + choferes.size() + " ciudadanos=" + ciudadanos.size() + " nodos=" + nodos.size());
    }

    // el correo queda vacio y se completa con el de gub.uy en su primer ingreso
    private UsuarioDto ciudadano(String cedula) {
        UsuarioDto ciudadano = usuarioDAO.getCiudadanoByCedula(cedula);
        return ciudadano != null ? ciudadano : usuarioDAO.addCiudadano(cedula, null);
    }

    // un viaje asignado y sin iniciar, para el segundo tramo de la demo (la balanza caida): el chofer puede tener
    // varios asignados, pero inicia uno a la vez
    private void viajeDeRespaldo(EmpresaDto empresa, UsuarioDto responsable, UsuarioDto chofer) {
        VehiculoDto vehiculo = vehiculoDAO.getVehiculoByMatricula(VEHICULO_RESPALDO);
        if (vehiculo == null || !vehiculo.getIdEmpresa().equals(empresa.getId()) || !viajeDAO.getActivosDelChofer(chofer.getId()).isEmpty()) {
            return;
        }
        // Montevideo a Salto: la misma ruta con la que tracking-1 reporta a SBB5678 (flotas/flota-sur.json), asi el
        // recorrido del viaje sigue la guia
        GuiaDto guia = guiaDAO.addGuia(new GuiaDto(LocalDate.now(), -34.9011, -56.1645, -31.3833, -57.9667, 12000,
                empresa.getId(), responsable.getId(), guiaDAO.getRubros().get(0).getId(), guiaDAO.getTiposCarga().get(0).getId()));
        viajeDAO.addViaje(new ViajeDto(guia.getId(), vehiculo.getId(), chofer.getId()));
    }

    private void registrar(NodoDemo nodo) {
        if (nodoDAO.getByIdentificador(nodo.identificador()) != null) {
            return;
        }
        NodoPerifericoDto dto = new NodoPerifericoDto(nodo.identificador(), nodo.tipo(), nodo.puntoAcceso());
        dto.setEstado(EstadoNodo.HABILITADO);
        nodoDAO.addNodo(dto, nodo.clave() == null ? null : cifrado.cifrar(nodo.clave(), nodo.identificador()));
    }

    // "51111111, 52222222" -> las cedulas, sin espacios ni vacias
    static List<String> cedulas(String valor) {
        List<String> cedulas = new ArrayList<>();
        if (valor == null) {
            return cedulas;
        }
        for (String parte : valor.split(",")) {
            if (!parte.isBlank()) {
                cedulas.add(parte.trim());
            }
        }
        return cedulas;
    }

    // separados por ';'. Un nodo de tracking es solo su identificador; uno de balanzas, identificador|url|clave
    static List<NodoDemo> nodos(String valor) {
        List<NodoDemo> nodos = new ArrayList<>();
        if (valor == null) {
            return nodos;
        }
        for (String parte : valor.split(";")) {
            if (parte.isBlank()) {
                continue;
            }
            String[] campos = parte.trim().split("\\|", -1);
            if (campos.length == 1) {
                nodos.add(new NodoDemo(campos[0].trim(), TipoNodo.TRACKING, null, null));
            } else if (campos.length == 3) {
                nodos.add(new NodoDemo(campos[0].trim(), TipoNodo.BALANZAS, campos[1].trim(), campos[2].trim()));
            } else {
                // sin mostrar el valor: puede traer una clave
                throw new IllegalStateException("DEMO_NODOS: cada nodo es 'identificador' o 'identificador|url|clave'");
            }
        }
        return nodos;
    }

    static int numero(String valor, int porDefecto) {
        return valor == null || valor.isBlank() ? porDefecto : Integer.parseInt(valor.trim());
    }

    // package-private para reemplazarlo en los tests
    String entorno(String variable) {
        return System.getenv(variable);
    }

    record NodoDemo(String identificador, TipoNodo tipo, String puntoAcceso, String clave) {
    }
}
