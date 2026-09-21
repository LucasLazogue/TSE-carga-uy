## Requisitos
- Docker
- Maven
- JDK 24
- Node 24
- CLI oc (solo para apuntar al remoto)

## Ejecutar

Desde carga-uy ejecutar
- docker compose up

El dockerfile ya crea un usuario en el wildfly con credenciales u: admin p: admin123

Backoffice JSF corre en el puerto 8080:
- http://localhost:8080/carga-uy/

API REST:
- http://localhost:8080/carga-uy/api/empresas
- http://localhost:8080/carga-uy/api/vehiculos
- http://localhost:8080/carga-uy/api/permisos

Alta asincronica: cola JMS queue_alta_empresa, mensaje de texto
nroEmpresa|nombrePublico|razonSocial|direccionPrincipal

### Remoto
Una vez desplegada en OpenShift, obtener la URL con
- oc get route carga-uy

## Ejecutar aplicacion consola
Desde carga-uy-client ejecutar
- mvn compile "exec:java" "-Dexec.mainClass=tse.cargauy.App"

### Remoto
El cliente se conecta por EJB remoto a localhost:8080, asi que hay que hacer un tunel al pod.

Bajar el docker compose para liberar el puerto 8080 y ejecutar
- oc login --token=TOKEN --server=SERVER
- oc project lucaslaz-dev
- oc port-forward svc/carga-uy 8080:8080

El token sale de la consola de OpenShift, en Copy login command.

Dejar el tunel corriendo y levantar el cliente en otra terminal. Si el Service no tiene
endpoints, usar oc port-forward deploy/carga-uy 8080:8080

## Ejecutar frontend
Desde frontend ejecutar
- npm install
- npm run dev

Corre en el puerto 5173:
- http://localhost:5173/

La URL del backend se configura en el archivo .env (ver .env.example)

### Remoto
Crear un archivo .env.local (no se commitea) con la URL de OpenShift:
- VITE_API_TARGET=https://URL_DE_LA_RUTA/carga-uy

Reiniciar npm run dev para que tome el cambio.
