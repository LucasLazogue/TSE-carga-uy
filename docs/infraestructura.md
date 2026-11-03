# Infraestructura

[Proyecto](../README.md) · [Seguridad](seguridad.md) · [Observabilidad](observabilidad.md)

El entorno local está definido en [carga-uy/docker-compose.yml](../carga-uy/docker-compose.yml).
Compose construye el central, los nodos y la plataforma de observabilidad, y conecta los servicios
mediante sus nombres en la red del proyecto. El [arranque mínimo](../README.md#ejecución-local)
incluye la preparación del `.env` y los comandos para iniciar el sistema.

## Servicios y puertos

Los puertos de la tabla corresponden al host. Entre contenedores se utilizan el nombre del servicio
y su puerto interno; por ejemplo, el central llega a PostgreSQL por `postgres:5432`.

| Servicio | Responsabilidad | Puertos publicados |
| --- | --- | --- |
| `carga-uy` | WildFly: API REST, backoffice JSF y EJB | `8080` aplicación; `9990` administración y salud |
| `postgres` | Base relacional del central | `5433` → `5432` |
| `mongo` | Almacenamiento de posiciones | `27018` → `27017` |
| `ldap` | Directorio del backoffice | `1389` → `3389` |
| `artemis` | Broker externo de tracking | `61617` JMS con TLS; `8161` consola |
| `observabilidad` | Colector, Prometheus, Tempo, Loki y Grafana | `3000` Grafana; `4317` OTLP gRPC; `4318` OTLP HTTP |
| `nodo-tracking` | Generación y publicación de posiciones | `8081` salud y métricas |
| `nodo-tracking-2` | Segundo nodo de tracking, con la otra mitad de la flota | `8084` salud y métricas |
| `balanzas-sur` / `balanzas-norte` | API de cada operador de balanzas | `8082` / `8083` → `8080` |
| `pg-balanzas-sur` / `pg-balanzas-norte` | Base propia de cada operador | Solo red interna, `5432` |

El central usa PostgreSQL, MongoDB, LDAP y Artemis. Tracking publica directamente al broker,
por lo que puede seguir entregando posiciones aunque WildFly esté detenido. La conexión del
central al broker usa `artemis:61616`, que no se publica en el host.

Cada nodo de balanzas tiene su propia base y arranca vacío. Los peajes, balanzas y lecturas de demo se
cargan desde el host con `alta.mjs` y `simular.mjs`, según la guía de
[balanzas](../nodo-balanzas/README.md#cómo-levantarlo-con-el-resto). El registro y la habilitación en el central se realizan
desde el backoffice, según las guías de [tracking](../nodo-tracking/README.md#cómo-levantarlo)
y [balanzas](../nodo-balanzas/README.md#agregar-un-nodo-nuevo).

El frontend se ejecuta por separado con Vite en el puerto `5173`. Android se conecta a la dirección
del central accesible desde el dispositivo; ver la [configuración móvil](../mobile/README.md#configuración).

## Persistencia

| Volumen | Contenido |
| --- | --- |
| `pgdata` | Datos relacionales del central, incluidos usuarios y registro de nodos |
| `mongodata` | Posiciones y estado de su procesamiento |
| `ldapdata` | Directorio, usuarios y grupos del backoffice |
| `artemisdata` | Estado persistente del broker de tracking |
| `balanzassurdata` / `balanzasnortedata` | Peajes, balanzas y pesajes de cada operador |

Recrear los contenedores conserva esos volúmenes. `docker compose down -v` los elimina, por lo que
también se pierden cuentas, registros de nodos y datos de prueba acumulados. El Compose no declara
un volumen persistente para la plataforma de observabilidad.

## Configuración del entorno

`carga-uy/.env` proporciona los valores que el Compose pasa a cada servicio. El
[archivo de ejemplo](../carga-uy/.env.example) contiene la configuración inicial; el Compose
define el conjunto completo de variables y sus valores predeterminados.

| Variables | Área |
| --- | --- |
| `GUBUY_*`, `CARGAUY_FRONTEND_URL`, `CARGAUY_MOBILE_REDIRECT`, `CARGAUY_API_URL` | Login y URLs públicas; ver [seguridad](seguridad.md#urls-del-login) |
| `CARGAUY_JWT_SECRET`, `CARGAUY_CLAVE_CIFRADO`, `LDAP_*` | Sesiones, cifrado y directorio; ver [seguridad](seguridad.md#secretos-y-cifrado) |
| `MONGO_URI`, `MONGO_DB` | Conexión al almacén de posiciones |
| `ARTEMIS_HOST`, `ARTEMIS_PORT`, `ARTEMIS_USUARIO`, `ARTEMIS_CLAVE` | Conexión del central al broker |
| `NODO_*` | Publicador y simulación de [tracking](../nodo-tracking/README.md#configuración) |
| `BALANZAS_SUR_CLAVE_*`, `BALANZAS_NORTE_CLAVE_*` | Credenciales que Compose pasa a los [nodos de balanzas](../nodo-balanzas/README.md#configuración) |
| `OTEL_*` | Exportación y muestreo; ver [observabilidad](observabilidad.md#exportación-y-muestreo) |
| `CARGAUY_ZONA_HORARIA` | Zona de los servicios, `America/Montevideo` por defecto |

Después de editar variables, aplicar los cambios desde `carga-uy`:

```sh
docker compose up -d
```

Para reconstruir imágenes tras cambios de código, agregar `--build`. Para inspeccionar el estado,
usar `docker compose ps -a`; los checks y logs se describen en
[observabilidad](observabilidad.md#salud-y-diagnóstico).

## Acceso a OpenShift

Estos comandos permiten acceder a un central ya desplegado. Requieren la CLI `oc`, el login
obtenido desde **Copy login command** en la consola de OpenShift y el proyecto correspondiente:

```sh
oc login --token=TOKEN --server=SERVER
oc project NOMBRE_DEL_PROYECTO
oc get route carga-uy
```

Para usar la ruta desde el frontend de desarrollo, configurar su
[backend remoto](../frontend/README.md#configurar-el-backend) y las [URLs del login](seguridad.md#urls-del-login).

### Túnel al central

Para herramientas que esperan el central en `localhost:8080`, liberar primero ese puerto.
Si lo ocupa el central local, detener el servicio desde `carga-uy`:

```sh
docker compose stop carga-uy
```

Abrir el túnel y dejarlo ejecutándose mientras se usa el cliente en otra terminal:

```sh
oc port-forward svc/carga-uy 8080:8080
```

Si el Service no tiene endpoints, se puede apuntar al deployment:

```sh
oc port-forward deploy/carga-uy 8080:8080
```

Para la consola EJB/JMS, revisar también sus
[limitaciones de autenticación](../carga-uy-client/README.md#estado-actual).

## Despliegue en OpenShift

El central, PostgreSQL y el directorio LDAP corren en un mismo proyecto de OpenShift. Los
manifiestos están en [carga-uy/openshift](../carga-uy/openshift/) y las claves se guardan en Secrets
del clúster, fuera del repositorio. Cada servicio tiene el mismo nombre que en el Compose, por lo que
el central los encuentra con sus valores por defecto.

Los nodos de tracking corren en otro proyecto y llegan al broker por la Route `artemis-nodos`; su
despliegue está en la [guía del nodo](../nodo-tracking/README.md#despliegue-en-openshift).

Los comandos requieren la sesión de `oc` iniciada como en [acceso a OpenShift](#acceso-a-openshift)
y se ejecutan desde `carga-uy`.

### Central y PostgreSQL

PostgreSQL se crea con la plantilla del catálogo de OpenShift, cuya imagen admite el usuario al azar
con el que la plataforma ejecuta los contenedores:

```sh
oc new-app postgresql-persistent -p DATABASE_SERVICE_NAME=postgres \
  -p POSTGRESQL_USER=cargauy -p POSTGRESQL_DATABASE=cargauy -p POSTGRESQL_PASSWORD=CLAVE_DE_LA_BASE
```

El Service queda como `postgres`, y el usuario y la base como `cargauy`: son los valores por defecto
del datasource en el [Dockerfile](../carga-uy/Dockerfile), así que el central solo necesita la clave.
Los datos quedan en un volumen persistente.

Las claves del central van en el Secret `carga-uy-secretos`:

```sh
oc create secret generic carga-uy-secretos \
  --from-literal=DB_PASSWORD=CLAVE_DE_LA_BASE \
  --from-literal=CARGAUY_JWT_SECRET=SECRETO_DE_AL_MENOS_32_BYTES \
  --from-literal=CARGAUY_CLAVE_CIFRADO=KEYSET_DE_TINK_EN_BASE64
```

`DB_PASSWORD` es la clave elegida para la base. `CARGAUY_JWT_SECRET` se puede generar con
`openssl rand -base64 48`, y `CARGAUY_CLAVE_CIFRADO` con
`tinkey create-keyset --key-template AES256_GCM --out-format binary`, codificado en base64. Las
credenciales de gub.uy se agregan después con
`oc set data secret/carga-uy-secretos GUBUY_CLIENT_ID=... GUBUY_CLIENT_SECRET=...`. Cada secreto
está descrito en [seguridad](seguridad.md#secretos-y-cifrado).

La configuración que no es secreta está en el ConfigMap de
[openshift/configuracion.yaml](../carga-uy/openshift/configuracion.yaml), con las
[URLs del login](seguridad.md#urls-del-login) apuntando al frontend en Vercel.
[openshift/carga-uy.yaml](../carga-uy/openshift/carga-uy.yaml) crea el Deployment, el Service y un
Route con TLS:

```sh
oc apply -f openshift/configuracion.yaml -f openshift/carga-uy.yaml
oc rollout status deployment/carga-uy --timeout=10m
oc get route carga-uy
```

Las versiones nuevas las despliega el workflow [Deploy central](../.github/workflows/deploy-central.yml)
en cada push a `release` que modifica `carga-uy`: construye la imagen, la publica en ghcr.io y cambia
la imagen del Deployment. No aplica los manifiestos; si cambian, se aplican a mano con el
`oc apply` anterior. La imagen de ghcr.io debe ser pública para que OpenShift la descargue sin
credenciales.

El workflow se autentica con una ServiceAccount con rol `edit`, que permite cambiar la imagen pero
no los permisos.

```sh
oc create serviceaccount github-deploy
oc policy add-role-to-user edit -z github-deploy
oc apply -f - <<'FIN'
apiVersion: v1
kind: Secret
metadata:
  name: github-deploy-token
  annotations:
    kubernetes.io/service-account.name: github-deploy
type: kubernetes.io/service-account-token
FIN
oc extract secret/github-deploy-token --keys=token --to=-
```

En el repositorio, en **Settings → Secrets and variables → Actions**, ese token va en el secret
`OPENSHIFT_TOKEN`; el servidor del `oc login` y el nombre del proyecto van en las variables
`OPENSHIFT_SERVER` y `OPENSHIFT_NAMESPACE`.

### Directorio LDAP

El directorio del backoffice usa la imagen `389ds/dirsrv:3.1` del Compose. No tiene workflow: se
aplica una vez a mano.

| Archivo | Contenido |
| --- | --- |
| [openshift/ldap.yaml](../carga-uy/openshift/ldap.yaml) | Volumen de 1 GiB, Deployment y Service `ldap` en el puerto `3389` |
| [ldap/inicializar.sh](../carga-uy/ldap/inicializar.sh) | Configuración inicial; equivale al `post_start` del Compose |
| [ldap/directorio.ldif](../carga-uy/ldap/directorio.ldif) | Ramas de usuarios y grupos, y los grupos `administrador` y `autoridad` vacíos |

Las claves no están en el repositorio. Se guardan en el Secret `ldap-secretos`, que se crea primero:

```sh
oc create secret generic ldap-secretos \
  --from-literal=DS_DM_PASSWORD=CLAVE_DEL_DIRECTORIO \
  --from-literal=LDAP_ADMIN_USUARIO=USUARIO_ADMIN \
  --from-literal=LDAP_ADMIN_PASSWORD=CLAVE_ADMIN
oc set data secret/carga-uy-secretos LDAP_DM_PASSWORD=CLAVE_DEL_DIRECTORIO
```

`DS_DM_PASSWORD` y `LDAP_DM_PASSWORD` deben tener el mismo valor, la clave del Directory Manager:
el directorio la toma de la primera y el central se conecta con la segunda. `LDAP_ADMIN_USUARIO` y `LDAP_ADMIN_PASSWORD` definen la
primera cuenta del grupo `administrador`, necesaria para entrar al backoffice y crear las demás.

Los scripts llegan al pod por el ConfigMap `ldap-inicio`, montado en `/inicio`:

```sh
oc create configmap ldap-inicio --from-file=ldap/directorio.ldif --from-file=ldap/inicializar.sh
oc apply -f openshift/ldap.yaml
oc rollout status deployment/ldap
```

Con el pod listo, inicializar el directorio:

```sh
oc exec deploy/ldap -- sh /inicio/inicializar.sh
```

El script crea el sufijo `dc=carga,dc=uy`, el bloqueo tras cinco intentos fallidos, las ramas y los
grupos, el administrador inicial y el índice para paginar usuarios. Lo que ya existe no se vuelve a
crear, por lo que puede ejecutarse de nuevo. Al terminar imprime los miembros de `administrador`,
entre los que debe aparecer el usuario del Secret. Todo queda en el volumen, así que no hace falta
repetirlo cuando OpenShift recrea el pod.

Por último, reiniciar el central para que lea `LDAP_DM_PASSWORD`. Llega al directorio por
`ldap://ldap:3389`, el valor por defecto de `LDAP_URL`, sin más configuración:

```sh
oc rollout restart deployment/carga-uy
oc rollout status deployment/carga-uy --timeout=10m
```

Si cambia `inicializar.sh` o `directorio.ldif`, recrear el ConfigMap y reiniciar el pod antes de
volver a ejecutar el script:

```sh
oc create configmap ldap-inicio --from-file=ldap/directorio.ldif --from-file=ldap/inicializar.sh --dry-run=client -o yaml | oc apply -f -
oc rollout restart deployment/ldap
```
