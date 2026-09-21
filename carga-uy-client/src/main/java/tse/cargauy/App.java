package tse.cargauy;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import tse.cargauy.dtos.EmpresaDto;
import tse.cargauy.dtos.PermisoDto;
import tse.cargauy.dtos.VehiculoDto;
import tse.cargauy.exceptions.CargaUYException;
import tse.cargauy.negocio.empresa.EmpresaEJBRemote;
import tse.cargauy.negocio.permiso.PermisoEJBRemote;
import tse.cargauy.negocio.vehiculo.VehiculoEJBRemote;

public class App {
    public static void main(String[] args) {

        Context ctx = null;
        EmpresaEJBRemote empresaEJB = null;
        VehiculoEJBRemote vehiculoEJB = null;
        PermisoEJBRemote permisoEJB = null;

        Properties props = new Properties();
        props.setProperty("java.naming.factory.initial", "org.wildfly.naming.client.WildFlyInitialContextFactory");
        props.setProperty("java.naming.provider.url", "remote+http://localhost:8080");
        props.setProperty(Context.SECURITY_PRINCIPAL, "admin");
        props.setProperty(Context.SECURITY_CREDENTIALS, "admin123");

        try {
            ctx = new InitialContext(props);
            String empresaJndiName = "carga-uy/tse.cargauy-carga-uy-ejb-1.0-SNAPSHOT/EmpresaEJB!tse.cargauy.negocio.empresa.EmpresaEJBRemote";
            empresaEJB = (EmpresaEJBRemote) ctx.lookup(empresaJndiName);
            String vehiculoJndiName = "carga-uy/tse.cargauy-carga-uy-ejb-1.0-SNAPSHOT/VehiculoEJB!tse.cargauy.negocio.vehiculo.VehiculoEJBRemote";
            vehiculoEJB = (VehiculoEJBRemote) ctx.lookup(vehiculoJndiName);
            String permisoJndiName = "carga-uy/tse.cargauy-carga-uy-ejb-1.0-SNAPSHOT/PermisoEJB!tse.cargauy.negocio.permiso.PermisoEJBRemote";
            permisoEJB = (PermisoEJBRemote) ctx.lookup(permisoJndiName);
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
                    listVehiculos(vehiculoEJB);
                    break;
                case 6:
                    listVehiculosByEmpresa(vehiculoEJB);
                    break;
                case 7:
                    createVehiculo(vehiculoEJB);
                    break;
                case 8:
                    listPermisosByVehiculo(permisoEJB);
                    break;
                case 9:
                    createPermiso(permisoEJB);
                    break;
                case 10:
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
        System.out.println("5. Listar vehiculos");
        System.out.println("6. Listar vehiculos por empresa");
        System.out.println("7. Crear nuevo vehiculo");
        System.out.println("8. Listar permisos de un vehiculo");
        System.out.println("9. Crear nuevo permiso");
        System.out.println("10. Salir");
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

    private static void listVehiculos(VehiculoEJBRemote vehiculoEJB) {
        List<VehiculoDto> vehiculos = vehiculoEJB.getAll();
        for (VehiculoDto vehiculo : vehiculos) {
            System.out.println(vehiculo);
        }
    }

    private static void listVehiculosByEmpresa(VehiculoEJBRemote vehiculoEJB) {
        try {
            System.out.print("Ingrese el id de la empresa: ");
            Long idEmpresa = Long.parseLong(System.console().readLine().trim());
            List<VehiculoDto> vehiculos = vehiculoEJB.getByEmpresa(idEmpresa);
            for (VehiculoDto vehiculo : vehiculos) {
                System.out.println(vehiculo);
            }
        } catch (NumberFormatException e) {
            System.out.println("El id de la empresa debe ser numerico.");
        }
    }

    private static void createVehiculo(VehiculoEJBRemote vehiculoEJB) {
        try {
            System.out.print("Ingrese la matricula: ");
            String matricula = System.console().readLine();
            System.out.print("Ingrese la marca: ");
            String marca = System.console().readLine();
            System.out.print("Ingrese el modelo: ");
            String modelo = System.console().readLine();
            System.out.print("Ingrese el peso del vehiculo en kg: ");
            int pesoVehiculo = Integer.parseInt(System.console().readLine().trim());
            System.out.print("Ingrese la capacidad de carga en kg: ");
            int capacidadCarga = Integer.parseInt(System.console().readLine().trim());
            System.out.print("Ingrese el id de la empresa: ");
            Long idEmpresa = Long.parseLong(System.console().readLine().trim());

            vehiculoEJB.addVehiculo(new VehiculoDto(matricula, marca, modelo, pesoVehiculo, capacidadCarga, idEmpresa));
            System.out.println("Vehiculo creado exitosamente.");
        } catch (NumberFormatException e) {
            System.out.println("El peso, la capacidad de carga y el id de la empresa deben ser numericos.");
        } catch (CargaUYException e) {
            System.out.println("Error al crear el vehiculo: " + e.getMessage());
        }
    }

    private static void listPermisosByVehiculo(PermisoEJBRemote permisoEJB) {
        try {
            System.out.print("Ingrese el id del vehiculo: ");
            Long idVehiculo = Long.parseLong(System.console().readLine().trim());
            List<PermisoDto> permisos = permisoEJB.getByVehiculo(idVehiculo);
            for (PermisoDto permiso : permisos) {
                System.out.println(permiso);
            }

            PermisoDto vigente = permisoEJB.getVigente(idVehiculo, LocalDate.now());
            if (vigente == null) {
                System.out.println("El vehiculo no tiene permiso vigente a la fecha.");
            } else {
                System.out.println("Permiso vigente: " + vigente.getNroPermiso());
            }
        } catch (NumberFormatException e) {
            System.out.println("El id del vehiculo debe ser numerico.");
        }
    }

    private static void createPermiso(PermisoEJBRemote permisoEJB) {
        try {
            System.out.print("Ingrese el numero de permiso: ");
            String nroPermiso = System.console().readLine();
            System.out.print("Ingrese la fecha de inicio de validez (aaaa-mm-dd): ");
            LocalDate validoDesde = LocalDate.parse(System.console().readLine().trim());
            System.out.print("Ingrese la fecha de fin de validez (aaaa-mm-dd): ");
            LocalDate validoHasta = LocalDate.parse(System.console().readLine().trim());
            System.out.print("Ingrese el id del vehiculo: ");
            Long idVehiculo = Long.parseLong(System.console().readLine().trim());

            permisoEJB.addPermiso(new PermisoDto(nroPermiso, validoDesde, validoHasta, idVehiculo));
            System.out.println("Permiso creado exitosamente.");
        } catch (DateTimeParseException e) {
            System.out.println("Las fechas deben tener el formato aaaa-mm-dd.");
        } catch (NumberFormatException e) {
            System.out.println("El id del vehiculo debe ser numerico.");
        } catch (CargaUYException e) {
            System.out.println("Error al crear el permiso: " + e.getMessage());
        }
    }
}
