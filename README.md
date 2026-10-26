# carga.uy

Sistema de gestión y fiscalización del transporte de carga. El componente central reúne la API REST,
el backoffice y la lógica de negocio; la web, la app Android y los nodos periféricos se conectan a él
para gestionar empresas, vehículos, permisos, viajes y datos de fiscalización.

## Componentes

| Directorio | Responsabilidad |
| --- | --- |
| [carga-uy](carga-uy/) | Backend Jakarta EE sobre WildFly: datos, EJB, API REST y backoffice JSF. Incluye el Compose del entorno local |
| [frontend](frontend/README.md) | Interfaz web en React y TypeScript |
| [mobile](mobile/README.md) | App Android para choferes, con trabajo sin conexión y sincronización |
| [nodo-tracking](nodo-tracking/README.md) | Simulación de flota y publicación de posiciones por JMS |
| [nodo-balanzas](nodo-balanzas/README.md) | Peajes, balanzas y API de pesajes de cada operador |
| [carga-uy-client](carga-uy-client/README.md) | Cliente de consola EJB/JMS; su guía detalla el estado de la autenticación remota |

## Requisitos

- Docker con Docker Compose para el central y sus servicios.
- Node.js 24 y npm para el frontend.
- Maven y JDK 24 para compilar o probar Java fuera de Docker.
- Para Android, los [requisitos de la app](mobile/README.md#requisitos).

## Ejecución local

Desde la raíz del repositorio, copiar [carga-uy/.env.example](carga-uy/.env.example) a
`carga-uy/.env`. En PowerShell:

```powershell
Copy-Item carga-uy/.env.example carga-uy/.env
```

En Bash, usar `cp carga-uy/.env.example carga-uy/.env`. Editar el archivo copiado para usar
el login simulado y crear el administrador inicial del backoffice:

```dotenv
GUBUY_MOCK=true
GUBUY_REDIRECT_URI=http://localhost:8080/carga-uy/api/auth/callback
CARGAUY_JWT_SECRET=secreto-local-solo-para-desarrollo
LDAP_ADMIN_USUARIO=admin-local
LDAP_ADMIN_PASSWORD=admin-local-123
```

Estos valores son de desarrollo. El archivo de ejemplo trae el login simulado desactivado.
Luego levantar el entorno:

```sh
cd carga-uy
docker compose up --build -d
```

El backoffice queda en http://localhost:8080/carga-uy/, con el usuario LDAP configurado arriba.
Los servicios, puertos y volúmenes están descritos en [infraestructura](docs/infraestructura.md).

En otra terminal, iniciar el [frontend](frontend/README.md#ejecutar-en-desarrollo). Con login
simulado, `11111111` corresponde al funcionario de prueba y `55555555` al chofer Carlos.
Para usar un celular o emulador, seguir la [configuración de Android](mobile/README.md#configuración).

El Compose incluye los nodos, pero su registro y habilitación en el central se realizan desde
el backoffice. Las guías de [tracking](nodo-tracking/README.md#cómo-levantarlo) y
[balanzas](nodo-balanzas/README.md#agregar-un-nodo-nuevo) explican ese paso y sus simulaciones.

## Documentación técnica

| Área | Contenido |
| --- | --- |
| [Seguridad](docs/seguridad.md) | Identidades, roles, sesiones, gub.uy, LDAP, secretos, cifrado y acceso de los nodos |
| [Observabilidad](docs/observabilidad.md) | Métricas, trazas, logs, muestreo, dashboards y diagnóstico |
| [Mensajería](docs/mensajeria.md) | Brokers, destinos JMS, contratos, persistencia, deduplicación y reentregas |
| [Infraestructura](docs/infraestructura.md) | Servicios, dependencias, puertos, volúmenes, configuración y acceso remoto |
| [Performance](docs/performance.md) | Volumen objetivo de tracking, cómo generar la carga y resultados de las pruebas |

## Verificación

Desde la raíz, ejecutar las pruebas y el análisis del central con `mvn -f carga-uy/pom.xml verify -Pci`.
Las verificaciones específicas están en los README de cada componente. El
[workflow de CI](.github/workflows/ci.yml) define los checks de Java, frontend, Android e imágenes
Docker que se ejecutan según los componentes modificados.
