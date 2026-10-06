package tse.cargauy.web.ws.nodos;

import java.util.List;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import tse.cargauy.dtos.NodoPerifericoDto;
import tse.cargauy.negocio.nodo.NodoPerifericoEJBLocal;

// TODO restringir al rol administrador cuando exista (CU-03)
@RequestScoped
@Path("/nodos")
@Consumes("application/json")
@Produces("application/json")
public class NodosRest {

    @EJB
    NodoPerifericoEJBLocal nodoEJB;

    public NodosRest() {
    }

    @GET
    public List<NodoPerifericoDto> getNodos() {
        return nodoEJB.getVigentes();
    }

    @Path("/{id}")
    @GET
    public NodoPerifericoDto getNodo(@PathParam("id") Long id) {
        return nodoEJB.getById(id);
    }

    @POST
    public void createNodo(NodoPerifericoDto nodoDto) {
        nodoEJB.addNodo(nodoDto);
    }

    // solo cambia el punto de acceso, y el nodo vuelve a quedar deshabilitado
    @Path("/{id}")
    @PUT
    public void updateNodo(@PathParam("id") Long id, NodoPerifericoDto nodoDto) {
        nodoEJB.cambiarPuntoAcceso(id, nodoDto.getPuntoAcceso());
    }

    @Path("/{id}/habilitar")
    @POST
    public void habilitar(@PathParam("id") Long id) {
        nodoEJB.habilitar(id);
    }

    @Path("/{id}/deshabilitar")
    @POST
    public void deshabilitar(@PathParam("id") Long id) {
        nodoEJB.deshabilitar(id);
    }

    @Path("/{id}")
    @DELETE
    public void deleteNodo(@PathParam("id") Long id) {
        nodoEJB.darDeBaja(id);
    }
}
