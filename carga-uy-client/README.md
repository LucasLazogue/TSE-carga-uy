# Cliente de consola

[Volver al proyecto](../README.md)

Cliente Java con acceso al central mediante EJB remoto y JMS para operaciones de empresas,
vehículos y permisos.

## Estado actual

La configuración de autenticación necesita adaptarse a los permisos actuales antes de usarlo
como flujo de prueba. [App.java](src/main/java/tse/cargauy/App.java) usa `admin` / `admin123`,
y el [Dockerfile del central](../carga-uy/Dockerfile) crea ese usuario de aplicación con el grupo
`guest`. Los EJB actuales exigen roles como `FUNCIONARIO` y `RESPONSABLE`.

Por esas diferencias de configuración, el usuario predeterminado no tiene los permisos para
las operaciones del menú. La compilación en CI comprueba el cliente, pero no valida su acceso
remoto a un servidor en ejecución.

## Compilar e invocar

Requiere Maven y JDK 24. Instalar primero los módulos del central en el repositorio Maven del host;
el build de Docker los instala dentro de la imagen. Desde la raíz del repositorio:

```sh
mvn -f carga-uy/pom.xml install -DskipTests
cd carga-uy-client
mvn verify
```

Una vez resuelta la autenticación, el comando de ejecución es:

```sh
mvn compile "exec:java" "-Dexec.mainClass=tse.cargauy.App"
```

El cliente usa `remote+http://localhost:8080`. Para llegar a un central en OpenShift, ver el
[túnel de acceso](../docs/infraestructura.md#túnel-al-central).

## Alta asíncrona de empresas

El menú permite publicar solicitudes en `queue_alta_empresa`. El contrato del mensaje y su
tratamiento en el central están documentados en [mensajería](../docs/mensajeria.md#contrato-de-alta-de-empresas).
