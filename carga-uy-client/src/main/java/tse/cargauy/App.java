package tse.cargauy;

import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.negocio.empresa.EmpresaEJBRemote;

public class App {
    public static void main(String[] args) {

        Context ctx = null;
        EmpresaEJBRemote empresaEJB = null;

        Properties props = new Properties();
        props.setProperty("java.naming.factory.initial", "org.wildfly.naming.client.WildFlyInitialContextFactory");
        props.setProperty("java.naming.provider.url", "remote+http://localhost:8080");
        props.setProperty(Context.SECURITY_PRINCIPAL, "admin");
        props.setProperty(Context.SECURITY_CREDENTIALS, "admin123");

        try {
            ctx = new InitialContext(props);
            String jndiName = "carga-uy/tse.cargauy-carga-uy-ejb-1.0-SNAPSHOT/EmpresaEJB!tse.cargauy.negocio.empresa.EmpresaEJBRemote";
            empresaEJB = (EmpresaEJBRemote) ctx.lookup(jndiName);
        } catch (NamingException e) {
            e.printStackTrace();
            return;
        }

        while (true) {
            showMenu();
            int option = Integer.parseInt(System.console().readLine());
            switch (option) {
                case 1:
                    listEmpresas(empresaEJB);
                    break;
                case 2:
                    findEmpresaByNombre(empresaEJB);
                    break;
                case 3:
                    createEmpresa(empresaEJB);
                    break;
                case 4:
                    createEmpresaCola(ctx);
                    break;
                case 5:
                    System.out.println("Saliendo del sistema...");
                    System.exit(0);
            }
        }
    }

    private static void showMenu() {
        System.out.println("Carga UY");
        System.out.println("Seleccione una opcion:");
        System.out.println("1. Listar empresas");
        System.out.println("2. Buscar empresa por nombre");
        System.out.println("3. Crear nueva empresa");
        System.out.println("4. Crear empresa por cola");
        System.out.println("5. Salir");
    }

    private static void listEmpresas(EmpresaEJBRemote empresaEJB) {
        List<EmpresaDto> empresas = empresaEJB.getAll();
        for (EmpresaDto empresa : empresas) {
            System.out.println(empresa);
        }
    }

    private static void findEmpresaByNombre(EmpresaEJBRemote empresaEJB) {
        System.out.print("Ingrese el nombre o razon social a buscar: ");
        String nombre = System.console().readLine();
        List<EmpresaDto> empresas = empresaEJB.findByNombre(nombre);
        for (EmpresaDto empresa : empresas) {
            System.out.println(empresa);
        }
    }

    private static EmpresaDto readEmpresa() {
        System.out.print("Ingrese el numero de empresa: ");
        int nroEmpresa = Integer.parseInt(System.console().readLine().trim());
        System.out.print("Ingrese el nombre publico: ");
        String nombrePublico = System.console().readLine();
        System.out.print("Ingrese la razon social: ");
        String razonSocial = System.console().readLine();
        System.out.print("Ingrese la direccion principal: ");
        String direccionPrincipal = System.console().readLine();

        EmpresaDto dto = new EmpresaDto();
        dto.setNroEmpresa(nroEmpresa);
        dto.setNombrePublico(nombrePublico);
        dto.setRazonSocial(razonSocial);
        dto.setDireccionPrincipal(direccionPrincipal);
        return dto;
    }

    private static void createEmpresa(EmpresaEJBRemote empresaEJB) {
        try {
            empresaEJB.addEmpresa(readEmpresa());
            System.out.println("Empresa creada exitosamente.");
        } catch (NumberFormatException e) {
            System.out.println("El numero de empresa debe ser numerico.");
        } catch (CargaUYException e) {
            System.out.println("Error al crear la empresa: " + e.getMessage());
        }
    }

    private static void createEmpresaCola(Context ctx) {
        try {
            ConnectionFactory connectionFactory = (ConnectionFactory) ctx.lookup("jms/RemoteConnectionFactory");
            Destination destination = (Destination) ctx.lookup("java:/jms/queue/queue_alta_empresa");

            EmpresaDto dto = readEmpresa();
            String message = String.join("|",
                    String.valueOf(dto.getNroEmpresa()),
                    dto.getNombrePublico(),
                    dto.getRazonSocial(),
                    dto.getDireccionPrincipal());

            try (JMSContext jmsContext = connectionFactory.createContext("admin", "admin123")) {
                jmsContext.createProducer().send(destination, message);
                System.out.println("Mensaje enviado a la cola para crear la empresa.");
            }
        } catch (NumberFormatException e) {
            System.out.println("El numero de empresa debe ser numerico.");
        } catch (NamingException e) {
            System.out.println("Error al enviar el mensaje a la cola: " + e.getMessage());
        }
    }
}
