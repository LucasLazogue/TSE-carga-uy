# carga.uy

[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=LucasLazogue_TSE-carga-uy&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=LucasLazogue_TSE-carga-uy)
[![Cobertura](https://sonarcloud.io/api/project_badges/measure?project=LucasLazogue_TSE-carga-uy&metric=coverage)](https://sonarcloud.io/summary/new_code?id=LucasLazogue_TSE-carga-uy)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=LucasLazogue_TSE-carga-uy&metric=bugs)](https://sonarcloud.io/summary/new_code?id=LucasLazogue_TSE-carga-uy)
[![Vulnerabilidades](https://sonarcloud.io/api/project_badges/measure?project=LucasLazogue_TSE-carga-uy&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=LucasLazogue_TSE-carga-uy)

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
| [pdi-suplantador](pdi-suplantador/README.md) | Servicio SOAP que suplanta al Servicio Básico de Información de DNIC, con personas ficticias |
| [carga-uy-client](carga-uy-client/README.md) | Cliente de consola EJB/JMS; su guía detalla el estado de la autenticación remota |

## Requisitos

- Docker con Docker Compose para el entorno local.
- Node.js 24 y npm para desarrollar el frontend y cargar los datos de los nodos de balanzas.
- Maven y JDK 24 para compilar o probar Java fuera de Docker.
- Para Android, los [requisitos de la app](mobile/README.md#requisitos).

## Ejecución local

Desde la raíz del repositorio, con Docker en ejecución:

```sh
docker compose -f carga-uy/docker-compose.yml up --build -d
```

El comando construye y levanta todos los componentes: el central, el frontend, los nodos, el
suplantador de la PDI y la plataforma de observabilidad. La primera vez tarda varios minutos en
construir las imágenes.

| Componente | Dirección |
| --- | --- |
| Frontend | http://localhost:5173/ |
| Backoffice | http://localhost:8080/carga-uy/, con el usuario `admin-local` y la clave `admin-local-123` |
| Grafana | http://localhost:3000/ |

El login de gub.uy es simulado: `11111111` corresponde al funcionario de prueba y `55555555` al
chofer Carlos. Los nodos de tracking y de balanzas quedan registrados y habilitados en el central.
Los peajes, balanzas y lecturas de demo se cargan con los
[scripts de balanzas](nodo-balanzas/README.md#cómo-levantarlo-con-el-resto). Los servicios, puertos
y volúmenes están descritos en [infraestructura](docs/infraestructura.md).

Estos valores son de desarrollo y no hace falta configurar nada. Para cambiarlos, por ejemplo para
usar [gub.uy de testing](docs/seguridad.md#gubuy-de-testing) u otro
[perfil de datos](docs/infraestructura.md#datos-del-central), copiar
[carga-uy/.env.example](carga-uy/.env.example) a `carga-uy/.env` y definir ahí las variables.
Para trabajar en el frontend con Vite, ver su [README](frontend/README.md#ejecutar-en-desarrollo).
Para usar un celular o emulador, seguir la [configuración de Android](mobile/README.md#configuración).

## Documentación técnica

| Área | Contenido |
| --- | --- |
| [Seguridad](docs/seguridad.md) | Identidades, roles, sesiones, gub.uy, LDAP, secretos, cifrado y acceso de los nodos |
| [Observabilidad](docs/observabilidad.md) | Métricas, trazas, logs, muestreo, dashboards y diagnóstico |
| [Mensajería](docs/mensajeria.md) | Brokers, destinos JMS, contratos, persistencia, deduplicación y reentregas |
| [Infraestructura](docs/infraestructura.md) | Servicios, dependencias, puertos, volúmenes, configuración, acceso remoto y despliegue en OpenShift |
| [Performance](docs/performance.md) | Volumen objetivo de tracking, cómo generar la carga y resultados de las pruebas |
| [Registro de decisiones](docs/registro-de-decisiones.md) | Siete decisiones de arquitectura con contexto, alternativas descartadas y consecuencias |

## Verificación

Desde la raíz, ejecutar las pruebas y el análisis del central con `mvn -f carga-uy/pom.xml verify -Pci`.
Las verificaciones específicas están en los README de cada componente. El
[workflow de CI](.github/workflows/ci.yml) define los checks de Java, frontend, Android e imágenes
Docker que se ejecutan según los componentes modificados.
