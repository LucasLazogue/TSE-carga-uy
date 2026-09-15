package tse.cargauy.negocio.empresa;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.exceptions.CargaUYException;

// formato del mensaje: nroEmpresa|nombrePublico|razonSocial|direccionPrincipal
@MessageDriven(mappedName = "EmpresaQueueMDB", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/queue/queue_alta_empresa"),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")
})
public class EmpresaQueueMDB implements MessageListener {

    @EJB
    private EmpresaEJBLocal empresaEJB;

    @Override
    public void onMessage(Message message) {
        try {
            if (!(message instanceof TextMessage msg)) {
                System.err.println("Mensaje no valido: no es de texto");
                return;
            }

            String[] parts = msg.getText().split("\\|");
            if (parts.length < 4) {
                System.err.println("Mensaje no valido: " + msg.getText());
                return;
            }

            empresaEJB.addEmpresa(partsToDto(parts));
        } catch (JMSException e) {
            e.printStackTrace();
        } catch (NumberFormatException | CargaUYException e) {
            // no se relanza para que el mensaje no se reintente indefinidamente
            System.err.println("No se pudo dar de alta la empresa: " + e.getMessage());
        }
    }

    private EmpresaDto partsToDto(String[] parts) {
        EmpresaDto empresaDto = new EmpresaDto();
        empresaDto.setNroEmpresa(Integer.parseInt(parts[0].trim()));
        empresaDto.setNombrePublico(parts[1]);
        empresaDto.setRazonSocial(parts[2]);
        empresaDto.setDireccionPrincipal(parts[3]);
        return empresaDto;
    }

}
