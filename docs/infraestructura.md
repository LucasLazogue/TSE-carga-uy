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
| `balanzas-sur` / `balanzas-norte` | API de cada operador de balanzas | `8082` / `8083` → `8080` |
| `pg-balanzas-sur` / `pg-balanzas-norte` | Base propia de cada operador | Solo red interna, `5432` |

El central usa PostgreSQL, MongoDB, LDAP y Artemis. Tracking publica directamente al broker,
por lo que puede seguir entregando posiciones aunque WildFly esté detenido. La conexión del
central al broker usa `artemis:61616`, que no se publica en el host.

Cada nodo de balanzas tiene su propia base. `balanzas-alta` espera los health checks de ambos nodos,
crea peajes y balanzas, y guarda sus claves; después `simulador-balanzas` carga las lecturas de demo.
Ambos son servicios de ejecución única. El registro y la habilitación en el central se realizan
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
| `balanzasclaves` | Claves generadas por el alta de balanzas y usadas por el simulador |

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
