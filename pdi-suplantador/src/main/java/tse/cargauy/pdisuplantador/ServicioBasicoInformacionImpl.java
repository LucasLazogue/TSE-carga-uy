package tse.cargauy.pdisuplantador;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.jws.WebService;
import tse.cargauy.pdisuplantador.soap.ListaMensajes;
import tse.cargauy.pdisuplantador.soap.Mensaje;
import tse.cargauy.pdisuplantador.soap.ObjPersona;
import tse.cargauy.pdisuplantador.soap.ObtProductInfo;
import tse.cargauy.pdisuplantador.soap.ParamObtPersonaPorDoc;
import tse.cargauy.pdisuplantador.soap.ParamProductDesc;
import tse.cargauy.pdisuplantador.soap.ResultObtPersonaPorDoc;
import tse.cargauy.pdisuplantador.soap.ResultObtProductDesc;
import tse.cargauy.pdisuplantador.soap.ServicioBasicoInformacionPortType;

@WebService(
        serviceName = "ServicioBasicoInformacion",
        portName = "ServicioBasicoInformacionPort",
        targetNamespace = "urn:carga-uy:suplantador-pdi:dnic:servicio-basico-informacion",
        endpointInterface = "tse.cargauy.pdisuplantador.soap.ServicioBasicoInformacionPortType",
        wsdlLocation = "WEB-INF/wsdl/ServicioBasicoInformacion.wsdl")
public class ServicioBasicoInformacionImpl implements ServicioBasicoInformacionPortType {

    private static final String TIPO_CEDULA = "DO";
    private static final int ORIENTAL = 1;

    private static final String CEDULA_SIN_SIGNOS = "\\d{6,8}";

    @Override
    public ResultObtPersonaPorDoc obtPersonaPorDoc(ParamObtPersonaPorDoc parametros) {
        demorar();
        if (Boolean.parseBoolean(entorno("PDI_FALLA"))) {
            return conError(CodigoDnic.CONSULTA_INCOMPLETA);
        }
        if (!parametrosValidos(parametros)) {
            return conError(CodigoDnic.PARAMETROS_INCORRECTOS);
        }
        if (!autorizado(parametros)) {
            return conError(CodigoDnic.ACCESO_NO_AUTORIZADO);
        }

        String cedula = parametros.getNroDocumento();
        CodigoDnic error = CasosEspeciales.error(cedula);
        if (error != null) {
            return conError(error);
        }
        ObjPersona persona = CasosEspeciales.buscar(cedula, LocalDate.now());
        if (persona == null) {
            persona = PersonasArchivo.buscar(entorno("PDI_PERSONAS_ARCHIVO"), cedula);
        }
        if (persona == null) {
            return conError(CodigoDnic.PERSONA_INEXISTENTE);
        }

        ResultObtPersonaPorDoc resultado = new ResultObtPersonaPorDoc();
        resultado.setObjPersona(persona);
        if (CasosEspeciales.aRegularizar(cedula)) {
            resultado.setWarnings(lista(CodigoDnic.DATOS_A_REGULARIZAR));
        }
        return resultado;
    }

    @Override
    public ResultObtProductDesc productDesc(ParamProductDesc parametros) {
        ObtProductInfo info = new ObtProductInfo();
        info.setVersion("1.0");
        info.setModalidad("Testing");
        info.setDescripcion("Servicio de Informacion D.N.I.C.");
        ResultObtProductDesc resultado = new ResultObtProductDesc();
        resultado.setObtProductInfo(info);
        return resultado;
    }

    // aparte para poder reemplazarlo en los tests
    String entorno(String variable) {
        return System.getenv(variable);
    }

    private static boolean parametrosValidos(ParamObtPersonaPorDoc parametros) {
        return parametros != null
                && parametros.getNroDocumento() != null
                && parametros.getNroDocumento().matches(CEDULA_SIN_SIGNOS)
                && TIPO_CEDULA.equals(parametros.getTipoDocumento());
    }

    private boolean autorizado(ParamObtPersonaPorDoc parametros) {
        String password = entorno("PDI_PASSWORD_ENTIDAD");
        return password != null && !password.isBlank()
                && password.equals(parametros.getPasswordEntidad())
                && parametros.getOrganizacion() != null
                && parametros.getOrganizacion().equals(entorno("PDI_ORGANIZACION"));
    }

    // simula una PDI que tarda en responder, para probar la espera del central
    private void demorar() {
        String demora = entorno("PDI_DEMORA_MS");
        if (demora == null || demora.isBlank()) {
            return;
        }
        try {
            Thread.sleep(Long.parseLong(demora.trim()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static ObjPersona persona(String cedula, String nombre1, String nombre2, String apellido1, String apellido2,
            Integer sexo, String fechaNacimiento) {
        ObjPersona persona = new ObjPersona();
        persona.setCodTipoDocumento(TIPO_CEDULA);
        persona.setNroDocumento(cedula);
        persona.setNombre1(nombre1);
        persona.setNombre2(nombre2);
        persona.setApellido1(apellido1);
        persona.setApellido2(apellido2);
        persona.setSexo(sexo);
        persona.setFechaNacimiento(fechaNacimiento);
        persona.setCodNacionalidad(ORIENTAL);
        persona.setNombreEnCedula(nombre1 + " " + apellido1);
        return persona;
    }

    // los errores van en el cuerpo de la respuesta, no como SOAP Fault: asi lo define el servicio real
    private static ResultObtPersonaPorDoc conError(CodigoDnic codigo) {
        ResultObtPersonaPorDoc resultado = new ResultObtPersonaPorDoc();
        resultado.setErrores(lista(codigo));
        return resultado;
    }

    private static ListaMensajes lista(CodigoDnic codigo) {
        Mensaje mensaje = new Mensaje();
        mensaje.setCodMensaje(codigo.getCodigo());
        mensaje.setDescripcion(codigo.getDescripcion());
        mensaje.setDatoExtra(UUID.randomUUID().toString());
        ListaMensajes lista = new ListaMensajes();
        lista.getMensajes().add(mensaje);
        return lista;
    }
}
