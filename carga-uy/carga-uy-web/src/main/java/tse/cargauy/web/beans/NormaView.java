package tse.cargauy.web.beans;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import tse.cargauy.dtos.NormaFiscalizacionDto;
import tse.cargauy.entities.NormaFiscalizacion;
import tse.cargauy.negocio.fiscalizacion.NormaFiscalizacionEJBLocal;

@Named
@ViewScoped
public class NormaView implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Map<String, String> NOMBRES = Map.of(
            NormaFiscalizacion.TOLERANCIA_EXCESO_CARGA, "Tolerancia de exceso de carga",
            NormaFiscalizacion.DESVIO_RUTA_DISTANCIA, "Distancia máxima de desvío de ruta",
            NormaFiscalizacion.PLAZO_DESCARGO_DIAS, "Plazo de descargo");
    private static final Map<String, String> UNIDADES = Map.of(
            NormaFiscalizacion.TOLERANCIA_EXCESO_CARGA, "%",
            NormaFiscalizacion.DESVIO_RUTA_DISTANCIA, "m",
            NormaFiscalizacion.PLAZO_DESCARGO_DIAS, "días");

    @EJB
    private NormaFiscalizacionEJBLocal normaEJB;

    List<NormaFiscalizacionDto> vigentes;
    String clave;
    Integer valor;
    LocalDate vigenteDesde;

    private final ModeloPaginado<NormaFiscalizacionDto> historial =
            new ModeloPaginado<>(paginacion -> normaEJB.getHistorial(clave, paginacion));

    @PostConstruct
    public void init() {
        cargar();
    }

    public void cargar() {
        vigentes = normaEJB.getVigentes();
    }

    public void editar(NormaFiscalizacionDto norma) {
        clave = norma.getClave();
        valor = norma.getValor();
        vigenteDesde = getHoy();
    }

    public void verHistorial(NormaFiscalizacionDto norma) {
        clave = norma.getClave();
    }

    public void guardar() {
        normaEJB.cambiarValor(clave, valor, vigenteDesde);
        mensaje(nombre(clave) + ": " + valor + " " + unidad(clave) + " desde el " + FECHA.format(vigenteDesde) + ".");
        cargar();
    }

    public String nombre(String clave) {
        return NOMBRES.get(clave);
    }

    public String unidad(String clave) {
        return UNIDADES.get(clave);
    }

    public LocalDate getHoy() {
        return LocalDate.now();
    }

    public List<NormaFiscalizacionDto> getVigentes() {
        return vigentes;
    }

    public ModeloPaginado<NormaFiscalizacionDto> getHistorial() {
        return historial;
    }

    public String getClave() {
        return clave;
    }

    public Integer getValor() {
        return valor;
    }

    public void setValor(Integer valor) {
        this.valor = valor;
    }

    public LocalDate getVigenteDesde() {
        return vigenteDesde;
    }

    public void setVigenteDesde(LocalDate vigenteDesde) {
        this.vigenteDesde = vigenteDesde;
    }

    private static void mensaje(String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, texto, null));
    }
}
