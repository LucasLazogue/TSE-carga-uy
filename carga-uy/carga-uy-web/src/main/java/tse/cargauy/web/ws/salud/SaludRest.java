package tse.cargauy.web.ws.salud;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tse.cargauy.data.posicion.AlmacenPosiciones;

// Verificacion de estado de la aplicacion (AC22): 200 si puede atender pedidos, 503 si no. Complementa los health
// checks del servidor (/health en el puerto de administracion), que no saben de las bases de la aplicacion.
// El broker no esta a proposito: sin el la API sigue funcionando y las posiciones esperan en el nodo
@RequestScoped
@Path("/salud")
@Produces(MediaType.APPLICATION_JSON)
public class SaludRest {

    private static final String UP = "UP";
    private static final String DOWN = "DOWN";

    @Resource(lookup = "java:jboss/datasources/CargaUyDS")
    DataSource datos;

    @EJB
    AlmacenPosiciones almacen;

    @GET
    public Response salud() {
        String baseDeDatos = baseDeDatos() ? UP : DOWN;
        // segun lo ultimo que vio el monitor del cliente de MongoDB: no espera una respuesta, asi no se traba
        String almacenPosiciones = almacen.conectado() ? UP : DOWN;
        boolean arriba = UP.equals(baseDeDatos) && UP.equals(almacenPosiciones);
        JsonObject cuerpo = Json.createObjectBuilder()
                .add("estado", arriba ? UP : DOWN)
                .add("chequeos", Json.createObjectBuilder()
                        .add("base-de-datos", baseDeDatos)
                        .add("almacen-posiciones", almacenPosiciones))
                .build();
        return Response.status(arriba ? Response.Status.OK : Response.Status.SERVICE_UNAVAILABLE).entity(cuerpo).build();
    }

    private boolean baseDeDatos() {
        try (Connection conexion = datos.getConnection()) {
            return conexion.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
}
