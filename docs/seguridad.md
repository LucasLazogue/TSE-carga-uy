# Seguridad

[Proyecto](../README.md) · [Infraestructura](infraestructura.md)

La seguridad del sistema combina la identidad de las personas, los permisos sobre las operaciones
y las credenciales de comunicación entre componentes. Esta referencia describe la implementación
actual y su configuración; el arranque local está en el [README](../README.md#ejecución-local).

## Identidades y autorización

| Acceso | Autenticación | Roles o permisos |
| --- | --- | --- |
| Frontend | gub.uy; simulación por cédula en desarrollo | `CIUDADANO`, `CHOFER`, `RESPONSABLE` y `FUNCIONARIO`, según el usuario registrado en el central |
| Android | gub.uy; simulación por cédula en desarrollo | El token móvil contiene únicamente `CHOFER`, o ningún rol si el usuario no es chofer |
| Backoffice JSF | Usuario y contraseña contra LDAP | Grupos `administrador` y `autoridad`, mapeados a `ADMINISTRADOR` y `AUTORIDAD` |
| Administración de WildFly | Usuario del servidor | Administración del servidor, independiente de las cuentas del backoffice |
| Nodo de tracking | Credenciales JMS y conexión TLS | Publicación en `queue_tracking` |
| API de balanzas | Clave en `Authorization: Bearer` | Administración del nodo, envío de pesajes o consulta, según la clave |

En el central, `JwtAuthenticationMechanism` valida el JWT recibido por cookie o encabezado Bearer
y entrega al contenedor el identificador del usuario y sus roles. Los EJB aplican `@RolesAllowed`
y, cuando corresponde, verifican el acceso a los datos de la empresa. Registrar, habilitar o
modificar nodos requiere `ADMINISTRADOR`.

El tablero de reportes (`/autoridad/*` en `web.xml`) y `InformeEJB` requieren `AUTORIDAD`. Cada
autoridad solo ve y descarga sus propias exportaciones; la de otra se responde como inexistente. La
consulta del tablero, cada exportación y cada descarga quedan en el log con el usuario y los filtros.

El administrador inicial de LDAP se crea con `LDAP_ADMIN_USUARIO` y `LDAP_ADMIN_PASSWORD` al
arrancar el servicio. El alta se ejecuta si la cuenta no existe; cambiar esas variables no actualiza
la contraseña de una cuenta persistida. El Compose configura bloqueo tras cinco intentos fallidos,
durante 900 segundos. El backoffice permite administrar las cuentas y sus grupos.

El Dockerfile crea también `admin` / `admin123` para WildFly. El usuario de aplicación con ese
nombre pertenece a `guest`; las consecuencias para EJB remoto están documentadas en el
[cliente de consola](../carga-uy-client/README.md#estado-actual).

## Sesiones web y móviles

La web y el backoffice usan la cookie HttpOnly `cargauy_token`. El JWT se firma con HS256 y
`CARGAUY_JWT_SECRET`, y contiene la identidad y los roles. La web cierra sesión mediante
`POST /api/auth/logout`, que elimina la cookie. La sesión web emitida tiene seis horas de vigencia.

Android inicia el login en `/api/auth/login?cliente=mobile&code_challenge=X`, donde `X` es el
SHA-256 en base64url de un `code_verifier` aleatorio. El navegador vuelve a
`cargauy://ingreso?code=C`; la app canjea ese código en `POST /api/auth/token` enviando
`grant_type=authorization_code`, `code`, `code_verifier` y una prueba en el encabezado `DPoP`.
La respuesta incluye `access_token`, `token_type`, `expires_in`, `refresh_token` y `refresh_expires_in`.

El token de acceso móvil dura 15 minutos y se presenta como `Authorization: Bearer` en la API.
La renovación usa `/api/auth/token` con `grant_type=refresh_token`, el refresh token y una prueba
DPoP de la misma clave del dispositivo. El central guarda el hash del refresh token y la huella
de esa clave. Su vigencia máxima es de 30 días, limitada además por siete días desde el último
uso registrado; el registro de uso se actualiza con una precisión de un día.

La renovación conserva el refresh token. Un nuevo login revoca los anteriores del mismo usuario
y dispositivo, y `POST /api/auth/revoke` permite revocarlo explícitamente. DPoP se verifica en el
canje y la renovación: firma ES256, método, URI y fecha dentro de una ventana de 60 segundos.
La implementación comprueba la presencia de `jti`, pero no conserva un registro de pruebas usadas.

El contrato está en [AuthRest](../carga-uy/carga-uy-web/src/main/java/tse/cargauy/web/ws/auth/AuthRest.java),
la emisión de JWT en [UsuarioEJB](../carga-uy/carga-uy-ejb/src/main/java/tse/cargauy/negocio/usuario/UsuarioEJB.java)
y el ciclo del refresh token en [SesionMovilEJB](../carga-uy/carga-uy-ejb/src/main/java/tse/cargauy/negocio/sesion/SesionMovilEJB.java).

## URLs del login

| Variable | Uso |
| --- | --- |
| `GUBUY_REDIRECT_URI` | Callback del central accesible desde el navegador |
| `CARGAUY_FRONTEND_URL` | Retorno del login web; por defecto `http://localhost:5173`. Su esquema determina si las cookies emitidas por la API exigen HTTPS |
| `CARGAUY_MOBILE_REDIRECT` | Retorno a Android; por defecto `cargauy://ingreso` |
| `CARGAUY_API_URL` | URL pública de la API para validar DPoP detrás de un proxy, por ejemplo `https://CENTRAL/carga-uy/api` |

En el flujo web, el host del callback debe coincidir con el que recibió la cookie de inicio del
login. Para probar por la IP de la máquina, abrir también el frontend por esa IP y configurar
`CARGAUY_FRONTEND_URL` con esa dirección. La configuración de red de Android está en su
[README](../mobile/README.md#configuración). `VITE_API_TARGET` configura únicamente el proxy de Vite.

Si el login falla, el retorno a la web o a Android incluye `?error=CODIGO`.

## gub.uy de testing

En `carga-uy/.env`, configurar las credenciales del grupo:

```dotenv
GUBUY_CLIENT_ID=<pedirlo al grupo>
GUBUY_CLIENT_SECRET=<pedirlo al grupo>
GUBUY_MOCK=false
GUBUY_REDIRECT_URI=http://localhost:8080
```

Desde `carga-uy`, aplicar el cambio con `docker compose up -d carga-uy`. Para esta configuración
de testing, agregar una vez la regla de WildFly que reenvía el retorno de la raíz al callback.
Ejecutar desde Bash en el mismo directorio:

```bash
MSYS_NO_PATHCONV=1 docker compose exec -T carga-uy /opt/jboss/wildfly/bin/jboss-cli.sh --connect --user=admin --password=admin123 <<'EOF'
/subsystem=undertow/configuration=filter/expression-filter=gubuy-callback:add(expression="path('/') and (exists('%{q,code}') or exists('%{q,error}')) -> redirect('/carga-uy/api/auth/callback%q')")
/subsystem=undertow/server=default-server/host=default-host/filter-ref=gubuy-callback:add
EOF
```

La regla queda en ese contenedor y debe aplicarse nuevamente si se recrea.
Para ingresar, crear una cuenta en https://mi-testing.iduruguay.gub.uy.
Para volver al simulado, restaurar `GUBUY_MOCK=true` y
`GUBUY_REDIRECT_URI=http://localhost:8080/carga-uy/api/auth/callback`, y aplicar el cambio con Compose.

## Secretos y cifrado

| Configuración | Responsabilidad |
| --- | --- |
| `CARGAUY_JWT_SECRET` | Firma de JWT; al menos 32 bytes |
| `CARGAUY_CLAVE_CIFRADO` | Keyset binario de Google Tink codificado en base64, usado para cifrar las claves de los nodos guardadas por el central |
| `GUBUY_CLIENT_SECRET` | Credencial del cliente OIDC de gub.uy |
| `LDAP_DM_PASSWORD` | Credencial de servicio para acceder al directorio LDAP |
| `NODO_JMS_USUARIO` / `NODO_JMS_CLAVE` | Credenciales del publicador de tracking |
| `NODO_CLAVE_ADMIN` / `NODO_CLAVE_CENTRAL` | Claves de administración y consulta de cada nodo de balanzas |

El central necesita recuperar la clave de consulta de un nodo, por eso la almacena cifrada con
AEAD. El identificador del nodo se usa como dato asociado: el texto cifrado queda ligado a ese
identificador. El keyset configurado debe conservarse para descifrar las claves ya guardadas.
La implementación está en [Cifrado](../carga-uy/carga-uy-ejb/src/main/java/tse/cargauy/seguridad/Cifrado.java).

Las claves individuales de las balanzas y los refresh tokens se verifican mediante hashes.
El Compose incluye secretos, claves y certificados de desarrollo; la configuración efectiva de
cada servicio está en [infraestructura](infraestructura.md#configuración-del-entorno).

## Seguridad de los nodos

Tracking se conecta al broker externo por TLS en el puerto `61617`, con el certificado de
confianza de `nodo-tracking/tls/confianza.p12`. Los permisos del broker permiten al nodo publicar
en `queue_tracking` y al central consumirla. La conexión interna del central usa `61616`.
La habilitación del nodo en la base del central es una comprobación adicional de la ingesta.

Balanzas autentica claves Bearer: la clave de administrador permite gestionar peajes y balanzas;
la clave de cada balanza permite enviar sus pesajes; `NODO_CLAVE_CENTRAL` permite consultar.
El nodo devuelve la clave individual al crear la balanza y almacena su hash. Los ejemplos locales
de la API usan HTTP; la conexión TLS documentada para tracking corresponde al transporte JMS.

Referencias: [permisos de Artemis](../carga-uy/artemis/etc/broker.xml) y
[autenticación de balanzas](../nodo-balanzas/src/main/java/tse/cargauy/nodobalanzas/seguridad/ClaveAuthenticationMechanism.java).

## Configuración de WildFly

El [Dockerfile](../carga-uy/Dockerfile) configura `integrated-jaspi=false` para aceptar la identidad
entregada por Jakarta Security. Si se ejecuta WildFly por separado, aplicar una vez con `jboss-cli`:

```text
/subsystem=undertow/application-security-domain=other:write-attribute(name=integrated-jaspi,value=false)
```

Ante un acceso rechazado, revisar la identidad recibida, sus roles y este ajuste. Para problemas
de callback, comprobar las URLs y el host de las cookies; para DPoP, la URL pública y el reloj del dispositivo.
