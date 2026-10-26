package tse.cargauy.nodobalanzas.negocio.peaje;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import tse.cargauy.nodobalanzas.data.peaje.PeajeDAOLocal;
import tse.cargauy.nodobalanzas.dtos.BalanzaDto;
import tse.cargauy.nodobalanzas.dtos.PeajeDto;
import tse.cargauy.nodobalanzas.exceptions.CodigoError;
import tse.cargauy.nodobalanzas.exceptions.NodoBalanzasException;
import tse.cargauy.nodobalanzas.seguridad.Rol;
import tse.cargauy.nodobalanzas.seguridad.Sha256;

@Stateless
@RolesAllowed(Rol.ADMINISTRADOR)
public class PeajeEJB implements PeajeEJBLocal {

    private static final SecureRandom RANDOM = new SecureRandom();

    @EJB
    PeajeDAOLocal peajeDAO;

    public List<PeajeDto> getPeajes() {
        return peajeDAO.getPeajes();
    }

    public void addPeaje(PeajeDto peajeDto) {
        if (vacio(peajeDto.getCodigo())) {
            throw new NodoBalanzasException(CodigoError.PEAJE_CODIGO_REQUERIDO);
        }
        if (vacio(peajeDto.getNombre())) {
            throw new NodoBalanzasException(CodigoError.PEAJE_NOMBRE_REQUERIDO);
        }
        if (vacio(peajeDto.getRuta())) {
            throw new NodoBalanzasException(CodigoError.PEAJE_RUTA_REQUERIDA);
        }
        if (peajeDto.getKm() < 0) {
            throw new NodoBalanzasException(CodigoError.PEAJE_KM_INVALIDO);
        }
        if (peajeDAO.getPeajeByCodigo(peajeDto.getCodigo()) != null) {
            throw new NodoBalanzasException(CodigoError.PEAJE_CODIGO_DUPLICADO, peajeDto.getCodigo());
        }
        peajeDAO.addPeaje(peajeDto);
    }

    public BalanzaDto addBalanza(String codigoPeaje, BalanzaDto balanzaDto) {
        if (peajeDAO.getPeajeByCodigo(codigoPeaje) == null) {
            throw new NodoBalanzasException(CodigoError.PEAJE_NO_ENCONTRADO, codigoPeaje);
        }
        if (vacio(balanzaDto.getCodigo())) {
            throw new NodoBalanzasException(CodigoError.BALANZA_CODIGO_REQUERIDO);
        }
        if (balanzaDto.getSentido() == null) {
            throw new NodoBalanzasException(CodigoError.BALANZA_SENTIDO_REQUERIDO);
        }
        if (peajeDAO.getBalanzaByCodigo(balanzaDto.getCodigo()) != null) {
            throw new NodoBalanzasException(CodigoError.BALANZA_CODIGO_DUPLICADO, balanzaDto.getCodigo());
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String clave = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        // no se guarda la clave, se guarda su hash: si alguien se roba la base no puede hacerse pasar por la balanza.
        // cuando la balanza manda un pesaje se hashea la clave que trae y se busca la balanza con ese hash.
        // no lleva salt como una contrasenia porque la clave la genera el nodo al azar, no se puede adivinar probando
        peajeDAO.addBalanza(codigoPeaje, balanzaDto, Sha256.base64Url(clave));
        return new BalanzaDto(balanzaDto.getCodigo(), balanzaDto.getSentido(), clave);
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
