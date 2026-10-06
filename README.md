## Requisitos
- Docker
- Maven
- JDK 24
- Node 24
- CLI oc (solo para apuntar al remoto)

## Ejecutar

Desde carga-uy copiar .env.example a .env y ejecutar
- docker compose up

El dockerfile ya crea un usuario en el wildfly con credenciales u: admin p: admin123

Backoffice JSF corre en el puerto 8080:
- http://localhost:8080/carga-uy/

API REST:
- http://localhost:8080/carga-uy/api/empresas
- http://localhost:8080/carga-uy/api/vehiculos
- http://localhost:8080/carga-uy/api/permisos

Login con gub.uy: usa el ambiente de testing de ID Uruguay y requiere GUBUY_CLIENT_ID y
GUBUY_CLIENT_SECRET en .env. La cedula 11111111 es un funcionario precargado; cualquier otra
entra como ciudadano.

Sin credenciales se puede simular con GUBUY_MOCK=true en .env: el login saltea gub.uy y entra
con la cedula 55555555 (chofer precargado), o con otra pasandola en /api/auth/login?cedula=22222222

La sesion es un JWT firmado con CARGAUY_JWT_SECRET. En la web viaja en la cookie HttpOnly
cargauy_token; el logout es POST /api/auth/logout.

Login mobile: la app abre en el navegador /api/auth/login?cliente=mobile&code_challenge=X, donde X es
el SHA-256 en base64url de un code_verifier aleatorio. Al terminar vuelve a cargauy://ingreso?code=C
(configurable con CARGAUY_MOBILE_REDIRECT) y la app hace POST /api/auth/token con
grant_type=authorization_code, code=C y code_verifier; la respuesta trae access_token, token_type y
expires_in, y el token se manda como Authorization: Bearer.

Si el login falla se vuelve al front o a la app con ?error=CODIGO (los de CodigoError).

Alta asincronica: cola JMS queue_alta_empresa, mensaje de texto
nroEmpresa|nombrePublico|razonSocial|direccionPrincipal

### Remoto
Una vez desplegada en OpenShift, obtener la URL con
- oc get route carga-uy

Nodos perifericos (CU-19): pantalla del backoffice en http://localhost:8080/carga-uy/nodos.xhtml y API
en /api/nodos. Posiciones recibidas del tracking (CU-10):
/api/posiciones?matricula=X. El simulador del nodo de tracking esta en nodo-tracking (ver su README).

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
