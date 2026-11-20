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
| `pdi-suplantador` | Servicio SOAP que suplanta a la PDI, sin base | `8085` → `8080` |

El central usa PostgreSQL, MongoDB, LDAP y Artemis. Tracking publica directamente al broker,
por lo que puede seguir entregando posiciones aunque WildFly esté detenido. La conexión del
central al broker usa `artemis:61616`, que no se publica en el host. El central no depende del
[suplantador de la PDI](../pdi-suplantador/README.md) para arrancar: sin él, los ciudadanos entran
sin los datos de la PDI y se completan en un próximo ingreso.

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
| `CARGAUY_CLAVE_VISTAS` | Cifrado del estado de las pantallas del backoffice; igual en todas las instancias. Ver [varias instancias](#varias-instancias-y-autoescalado) |
| `MONGO_URI`, `MONGO_DB` | Conexión al almacén de posiciones |
| `ARTEMIS_HOST`, `ARTEMIS_PORT`, `ARTEMIS_USUARIO`, `ARTEMIS_CLAVE` | Conexión del central al broker |
| `ARTEMIS_CONSOLA`, `ARTEMIS_CONSOLA_USUARIO`, `ARTEMIS_CONSOLA_CLAVE` | Consola del broker (Jolokia), de donde el central lee el tamaño de la cola para las [métricas](observabilidad.md) |
| `NODO_*` | Publicador y simulación de [tracking](../nodo-tracking/README.md#configuración) |
| `BALANZAS_SUR_CLAVE_*`, `BALANZAS_NORTE_CLAVE_*` | Credenciales que Compose pasa a los [nodos de balanzas](../nodo-balanzas/README.md#configuración) |
| `PDI_URL`, `PDI_ORGANIZACION`, `PDI_PASSWORD_ENTIDAD` | Conexión del central a la PDI: dirección y credenciales que manda |
| `SUPLANTADOR_ORGANIZACION`, `SUPLANTADOR_PASSWORD_ENTIDAD` | Credenciales que acepta el [suplantador de la PDI](../pdi-suplantador/README.md#configuración); tienen que coincidir con las del central |
| `SUPLANTADOR_DEMORA_MS`, `SUPLANTADOR_FALLA` | Simulación de fallas del [suplantador](../pdi-suplantador/README.md#probar-la-resiliencia-del-central) |
| `OTEL_*` | Exportación y muestreo; ver [observabilidad](observabilidad.md#exportación-y-muestreo) |
| `CARGAUY_ZONA_HORARIA` | Zona de los servicios, `America/Montevideo` por defecto |
| `CARGAUY_NODOS_HTTPS` | Exige `https` en el punto de acceso de los nodos de balanzas; `false` en Compose, `true` en OpenShift |

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

Los nodos de balanzas corren en otro proyecto, en otro clúster (`rm1`). El central los llama por HTTPS a
la dirección con la que está registrado cada nodo ([balanzas](balanzas.md)).

El suplantador de la PDI no está desplegado en OpenShift: sin `PDI_URL`, los ciudadanos entran sin los
datos de la PDI y sin control de mayoría de edad ([suplantador](../pdi-suplantador/README.md#despliegue)).

El Developer Sandbox escala a 0 los Deployments de un proyecto unas 12 horas después de levantarlos.
Para volver a usar el sistema hay que escalarlos de nuevo: primero los servicios y después el central,
que al arrancar necesita la base y el broker.

```sh
oc scale dc/postgres --replicas=1
oc scale deployment/mongo deployment/artemis deployment/ldap deployment/observabilidad --replicas=1
oc rollout status deployment/artemis
oc scale deployment/carga-uy --replicas=1
```

Los proyectos de los nodos se apagan igual y se levantan por separado. En el de los nodos de balanzas, primero las
bases y después los nodos: un nodo que arranca sin su base queda corriendo y figura como listo, pero sin la aplicación
desplegada, y responde 404 a todas las consultas del central (así pasó el 18/11):

```sh
oc scale dc/pg-balanzas-sur dc/pg-balanzas-norte --replicas=1
oc rollout status dc/pg-balanzas-sur && oc rollout status dc/pg-balanzas-norte
oc scale deployment/cargauy-balanzas-sur deployment/cargauy-balanzas-norte --replicas=1
```

El Service `artemis` publica también la consola (`8161`), solo dentro del proyecto: por ahí el central lee
cada 10 s los mensajes esperando y los consumidores de `queue_tracking`, y los mensajes muertos (`DLQ`). Se
autentica como el usuario `artemis` del broker, con `ARTEMIS_CONSOLA_USUARIO` (ConfigMap) y
`ARTEMIS_CONSOLA_CLAVE` (`oc set data secret/carga-uy-secretos ARTEMIS_CONSOLA_CLAVE=...`). Sin el puerto o con
otra clave, el panel "Cola de tracking" de Grafana queda vacío y el central lo avisa una vez en el log
(`metricas cola=SIN_DATOS`).

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

La plantilla deja PostgreSQL con 512 MiB y sin CPU propia, así que el Sandbox le aplica su límite por defecto de
1 CPU. Con varias instancias del central eso no alcanza: en la prueba de performance se quedó sin memoria con 4
instancias (`OOMKilled`), y con 2 CPU no se sostienen a la vez las 600 posiciones/s y el frontoffice
([performance](performance.md#qué-limitó-cada-corrida)). Hay que darle recursos propios:

```sh
oc set resources dc/postgres --limits=cpu=4,memory=3Gi --requests=cpu=250m,memory=512Mi
```

Con la carga del volumen objetivo usa unas 2,2 a 2,9 CPU y hasta 1,7 GB de memoria (con 2 GiB quedaba sin margen).
Aun así, el Sandbox puede terminar un proceso de PostgreSQL cuando el nodo compartido se queda sin memoria: el pod es
Burstable y la cuota del proyecto no alcanza para reservarle sus 4 CPU. Ver
[performance](performance.md#procesos-de-postgresql-terminados-en-el-sandbox). MongoDB queda con 2 CPU y 5 GiB de disco en su
[manifiesto](../carga-uy/openshift/mongo.yaml).

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

La clave del estado de las pantallas del backoffice va en un Secret aparte, `carga-uy-vistas`, que
el Deployment lee con `secretKeyRef`. Se crea **antes** de aplicar el manifiesto; el valor se genera
en el momento y no se muestra:

```sh
oc create secret generic carga-uy-vistas \
  --from-literal=CARGAUY_CLAVE_VISTAS="$(openssl rand -base64 32)"
```

Si el Secret no existe, la instancia nueva queda en `CreateContainerConfigError` y no arranca: la
anterior sigue atendiendo hasta que se crea el Secret. Más detalles en
[varias instancias](#varias-instancias-y-autoescalado).

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

### Varias instancias y autoescalado

El mismo manifiesto crea un HorizontalPodAutoscaler que mantiene entre 1 y 4 instancias del central
según el uso de CPU: agrega instancias cuando el promedio supera el 70 % de lo pedido (400m por
instancia) y las quita de a una después de 5 minutos por debajo. Lo calcula con el uso que mide la
plataforma, sin nada que instalar.

```sh
oc get hpa carga-uy --watch
```

El límite de 4 sale de la cuota del Developer Sandbox: 3 CPU de requests para todo el proyecto, de
las que Artemis, MongoDB, LDAP, PostgreSQL y la plataforma de observabilidad piden alrededor de 0,5.
Cuatro instancias más la extra que agrega un despliegue piden 2 CPU.

Mientras una instancia nueva arranca, que en WildFly lleva alrededor de un minuto, las posiciones
esperan en la cola de Artemis. Si una instancia se apaga con mensajes en proceso, sus transacciones
se deshacen y el broker se los entrega a otra; el procesamiento idempotente evita duplicados.

Lo que hace que el central funcione con varias instancias:

| Qué | Cómo se resuelve |
| --- | --- |
| Posiciones de tracking | Todas las instancias consumen de la misma cola y cada mensaje lo procesa una sola ([mensajería](mensajeria.md)). Un reenvío no duplica nada: índice único en MongoDB |
| Casos de fiscalización | La clave de deduplicación es única en la base. Si dos instancias detectan el mismo hecho a la vez, la segunda recibe `CASO_YA_REGISTRADO`, deshace lo suyo y no crea otro caso |
| Viajes | Bloqueo de fila al cambiar el estado y `@Version` para detectar cambios concurrentes |
| Reevaluación de pesajes pendientes | El temporizador corre en cada instancia, pero cada viaje lo toma una sola (`FOR UPDATE SKIP LOCKED`, ver [balanzas](balanzas.md#varias-instancias-del-central)) |
| Tareas nocturnas | Borrar refresh tokens viejos y vencer informes son `DELETE` y `UPDATE` idempotentes: da lo mismo que corran en una instancia o en todas |
| Preparación de la base al arrancar | Los triggers de la auditoría y los datos de prueba toman un cerrojo de PostgreSQL (`pg_advisory_xact_lock`): si dos instancias arrancan a la vez, la segunda espera y encuentra todo hecho |
| API (frontend y app) | Sin estado en el servidor: JWT firmado y DPoP sin guardar `jti` |
| Backoffice (JSF) | El login es el mismo JWT, en una cookie, que se valida en cada pedido. El estado de cada pantalla (el árbol de JSF y los beans `@ViewScoped` de OmniFaces con `saveInViewState`) viaja cifrado en la página, en `jakarta.faces.ViewState`, y no en la sesión: no se crea sesión HTTP. Cualquier instancia atiende el clic siguiente, así que el Route no tiene afinidad (`disable_cookies`) |
| Beans `@Singleton` | Hay uno por instancia, no uno en total. Ninguno guarda estado que tenga que ser único: el cliente de MongoDB y las métricas de la cola son por instancia, y la carga inicial se coordina con el cerrojo de PostgreSQL |
| Métricas de la cola | Cada instancia publica el tamaño de la cola; el tablero toma el máximo |

#### Estado de las pantallas del backoffice

JSF guarda el estado de cada pantalla en el cliente (`jakarta.faces.STATE_SAVING_METHOD=client`
en `web.xml`). Mojarra lo cifra con AES y lo firma con HMAC usando la clave de
`CARGAUY_CLAVE_VISTAS`, que llega a JSF por JNDI (`java:comp/env/faces/ClientSideSecretKey`, en
`WEB-INF/jboss-web.xml`). Todas las instancias tienen que usar la misma clave: si no, una no
puede leer las páginas que armó otra.

| `CARGAUY_CLAVE_VISTAS` | Qué pasa |
| --- | --- |
| Clave AES de 16, 24 o 32 bytes en base64 (`openssl rand -base64 32`) | Al arrancar se registra `estado de las pantallas cifrado con la clave de CARGAUY_CLAVE_VISTAS` |
| Vacía o sin definir | Aviso al arrancar: cada instancia usa una clave propia al azar. Con una sola instancia, como en el Compose de siempre, no cambia nada; con más de una, el clic que atiende otra instancia termina en «Página desactualizada» |
| Cualquier otro valor | El despliegue falla con un mensaje que nombra la variable, sin mostrar el valor, y `/health/ready` da `DOWN` |

Cambiar la clave invalida las páginas abiertas en ese momento. Lo mismo puede pasar con una página
abierta durante un despliegue, si la versión nueva cambia esa pantalla o los campos de su bean. En
los dos casos el clic siguiente muestra «Página desactualizada», con un botón a la pantalla de inicio
del rol y sin pedir login de nuevo.

Para probarlo con dos instancias, con `CARGAUY_CLAVE_VISTAS` en el `.env`:

```sh
docker compose -f docker-compose.yml -f docker-compose.dos-instancias.yml up -d --build \
  postgres mongo artemis ldap carga-uy carga-uy-2 balanceador
```

El backoffice queda en `http://localhost:8088/carga-uy/login.xhtml`, detrás de un balanceador sin
afinidad que alterna las instancias en cada pedido; el encabezado `X-Instancia` dice cuál respondió.
Con una pantalla abierta, `docker compose stop carga-uy` apaga una instancia y el clic siguiente lo
atiende la otra.

### Datos del central

El central carga sus datos al arrancar, y solo agrega lo que falta, así que reiniciarlo no duplica nada. Qué carga
depende del perfil, en `CARGAUY_DATOS`:

| Perfil | Qué hay | Para qué |
| --- | --- | --- |
| `demo` (por defecto) | Datos de referencia, datos de prueba y, si existe el Secret, el elenco | Desarrollo y la demo |
| `base` | Solo los datos de referencia (y los nodos del elenco, si existe el Secret) | Empezar sin empresas ni personas inventadas |
| `performance` | `base` más [performance/datos.sql](../performance/datos.sql): la flota del volumen objetivo | La [prueba de performance](performance.md#juego-de-datos) |

`performance` no es un valor de `CARGAUY_DATOS`: es `base` más el script, que se carga con el reinicio.

- **Datos de referencia** (`DatosPrueba`, en todos los perfiles): los roles, los rubros, los tipos de carga y las
  normas de fiscalización vigentes.
- **Datos de prueba** (`DatosPrueba`, solo en `demo`): empresas, vehículos con sus permisos e ITV, Guías, viajes y
  casos en cada estado, con personas ficticias. Incluyen cinco empresas más
  (1005 a 1009, `HistoriaPublica`) con un viaje terminado por mes de febrero a octubre: la información pública solo
  muestra un mes o un rubro con al menos 5 empresas y 5 vehículos detrás, y sin ellas Estadísticas quedaba vacía.
- **Elenco de la demo** (`ElencoDemo`): quién es funcionario, responsable o chofer, y los nodos periféricos
  registrados y habilitados. Usa las cédulas de las cuentas de gub.uy de testing del grupo, que son de personas
  reales, y las claves de los nodos: por eso no está en el repositorio sino en el Secret `carga-uy-demo`. Sin el
  Secret no hace nada.

| Variable | Contenido |
| --- | --- |
| `DEMO_FUNCIONARIOS` | Cédulas que entran como funcionarios, separadas por coma |
| `DEMO_RESPONSABLES` | Cédulas responsables de la empresa 1001 (`DEMO_EMPRESA` para otra) |
| `DEMO_CHOFERES` | Cédulas choferes de esa empresa. Cada uno recibe además un viaje asignado sin iniciar, con `SBB5678` |
| `DEMO_CIUDADANOS` | Cédulas que existen como ciudadanos, sin rol |
| `DEMO_NODOS` | Nodos separados por `;`: un nodo de tracking es su identificador; uno de balanzas, `identificador\|url\|clave` |

El correo de cada ciudadano queda vacío y se completa con el de gub.uy en su primer ingreso.

```sh
oc create secret generic carga-uy-demo \
  --from-literal=DEMO_FUNCIONARIOS=CEDULA \
  --from-literal=DEMO_RESPONSABLES=CEDULA \
  --from-literal=DEMO_CHOFERES=CEDULA \
  --from-literal=DEMO_CIUDADANOS=CEDULA \
  --from-literal='DEMO_NODOS=tracking-1;tracking-2;balanzas-sur|https://URL/nodo-balanzas|CLAVE_CENTRAL;balanzas-norte|https://URL/nodo-balanzas|CLAVE_CENTRAL'
```

Sin los datos de prueba, el elenco carga igual los nodos, los funcionarios y los ciudadanos; los responsables y los
choferes quedan sin empresa, porque la de la demo no existe.

[openshift/reiniciar-datos.sh](../carga-uy/openshift/reiniciar-datos.sh) deja el central como recién instalado, con el
perfil que se elija, para repetir la demo o una prueba desde el mismo punto:

```sh
./reiniciar-datos.sh                          # demo
./reiniciar-datos.sh --perfil base
./reiniciar-datos.sh --perfil performance
./reiniciar-datos.sh --perfil performance -- -v vehiculos=6000 -v meses=3   # parámetros de datos.sql
```

1. apaga el central;
2. borra y vuelve a crear la base de PostgreSQL. No alcanza con vaciar las tablas: los triggers de la auditoría no
   dejan borrar la pista;
3. borra las posiciones de MongoDB;
4. deja el perfil en el Deployment (`CARGAUY_DATOS`), donde queda hasta el próximo reinicio: un despliegue no lo
   cambia;
5. enciende el central, que crea el esquema y carga los datos del perfil;
6. con `performance`, carga `performance/datos.sql`.

Pide confirmación escribiendo el nombre del proyecto. No toca los usuarios de LDAP, las cuentas de gub.uy, la cola de
Artemis ni los pesajes de los nodos de balanzas. Después del reinicio, cualquier cuenta de gub.uy que no esté en el
elenco entra como ciudadano sin rol.

En Compose, el perfil y el elenco se definen en el `.env` con las mismas variables (`CARGAUY_DATOS`, `DEMO_*`). El
reinicio, desde `carga-uy`:

```sh
docker compose down -v && docker compose up -d                                   # demo
docker compose down -v && CARGAUY_DATOS=base docker compose up -d                # base
# performance: base y, con el central ya arrancado, el script
docker compose exec -T postgres psql -U cargauy -d cargauy < ../performance/datos.sql
```

Para que el central acepte las posiciones de la prueba, los nodos `tracking-1` y `tracking-2` tienen que estar
registrados y habilitados: en el perfil `base`, eso lo hace el elenco con `DEMO_NODOS`.

### Broker y almacén de posiciones

Artemis y MongoDB tienen sus manifiestos en [openshift/artemis.yaml](../carga-uy/openshift/artemis.yaml) y
[openshift/mongo.yaml](../carga-uy/openshift/mongo.yaml), con los comandos para crear sus Secrets
(`artemis-secretos` y `mongo-secretos`) en el encabezado de cada uno. La imagen del broker se construye en el
clúster a partir de `carga-uy/artemis`:

```sh
oc apply -f openshift/artemis.yaml -f openshift/mongo.yaml
oc start-build artemis --from-dir=artemis --follow
```

Los usuarios del broker de OpenShift vienen del Secret y reemplazan a los de desarrollo del repositorio. La clave
del usuario `central` va en `carga-uy-secretos` como `ARTEMIS_CLAVE`, y la de `artemis` como
`ARTEMIS_CONSOLA_CLAVE`. `MONGO_URI` usa el usuario root de `mongo-secretos`.

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
