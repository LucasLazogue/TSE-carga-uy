## Nodo periférico de balanzas

Simula el Sistema de Balanzas de un operador de peajes. Cada operador tiene su propio nodo, con su propia
base: sus peajes, las balanzas de cada peaje y los pesajes que registran. El componente central le pide al
nodo los pesajes de un vehículo en una fecha (consulta sincrónica, CU-11).

    Peaje (R5-30, Ruta 5 km 30)
     ├─ Balanza balanza-r5-km30   (hacia Montevideo)
     │    └─ Pesajes: SCC9012 20/10 09:10 36500 kg, ...
     └─ Balanza balanza-r5-km30-b (hacia el interior)

- El peaje tiene la ubicación (ruta, km, latitud y longitud). La balanza está donde está su peaje.
- Los pesajes no los carga una persona: los manda cada balanza, con su propia clave, cuando pasa un camión.
- Puede haber varios nodos (por ejemplo `balanzas-sur` y `balanzas-norte`): es la misma imagen con otra base
  y otras claves. Cada uno se registra en el central como nodo de tipo `BALANZAS`.

Es un WAR sobre WildFly 41, con la misma organización que el central: `entities`, `dtos`, `data`, `negocio`,
`ws`, `exceptions` y `seguridad`.

### Quién puede hacer qué

Todas las llamadas llevan `Authorization: Bearer <clave>`. Según la clave, el nodo sabe quién llama:

| Rol | Clave | Puede |
| --- | --- | --- |
| `ADMINISTRADOR` | `NODO_CLAVE_ADMIN` | Dar de alta peajes y balanzas, y listarlos |
| `BALANZA` | La que genera el nodo al dar de alta cada balanza | Enviar sus pesajes |
| `CENTRAL` | `NODO_CLAVE_CENTRAL` | Consultar pesajes |

Sin clave o con una clave que no existe: `401`. Con una clave válida pero sin permiso para esa operación: `403`.

### Cómo levantarlo con el resto

Junto al central, desde `carga-uy`:

    docker compose up --build -d

Levanta dos nodos, cada uno con su base, y los carga para la demo:

| Servicio | Qué hace |
| --- | --- |
| `balanzas-sur` (puerto 8082) y `balanzas-norte` (puerto 8083) | Los nodos, con sus bases `pg-balanzas-sur` y `pg-balanzas-norte` |
| `balanzas-alta` | Corre una vez: da de alta los peajes y balanzas de `datos/balanzas-sur.json` y `datos/balanzas-norte.json` con `scripts/alta.sh`, y guarda las claves que devuelve cada nodo en el volumen `balanzasclaves` |
| `simulador-balanzas` | Corre una vez, después del alta: hace de los equipos y manda las lecturas de `datos/pasadas-sur.json` y `datos/pasadas-norte.json` con `scripts/simular.sh`, cada balanza con su clave |

Las lecturas siguen los datos de prueba del central: SCC9012 con exceso de carga (36500 kg hace 3 días en
`balanza-r5-km30` y 35100 kg hace 7 días en `balanza-r1-km45`), STA1234 sin exceso en su viaje
Montevideo - Rivera del 15/10 (pesado en los dos nodos) y SBB5678 pesado sin viaje hace 2 días. Volver a
correrlos no duplica nada.

Las claves de desarrollo son `clave-admin-balanzas-sur`, `clave-central-balanzas-sur` y las mismas con
`norte`; se cambian con `BALANZAS_SUR_CLAVE_ADMIN`, `BALANZAS_SUR_CLAVE_CENTRAL`, etc.

### Cómo levantar un nodo suelto

Necesita una base PostgreSQL propia. Por ejemplo, con Docker (desde `nodo-balanzas`):

    docker network create balanzas
    docker run -d --name pg-balanzas --network balanzas -e POSTGRES_DB=balanzas -e POSTGRES_USER=balanzas -e POSTGRES_PASSWORD=balanzas postgres:17
    docker build -t nodo-balanzas .
    docker run -d --name balanzas-sur --network balanzas -p 8082:8080 -p 9992:9990 -e DB_HOST=pg-balanzas -e NODO_CLAVE_CENTRAL=<clave-central> -e NODO_CLAVE_ADMIN=<clave-admin> nodo-balanzas

Las tablas las crea Hibernate al arrancar. La API queda en `http://localhost:8082/nodo-balanzas/api`.

### Configuración

| Variable | Por defecto | Para qué |
| --- | --- | --- |
| `NODO_CLAVE_CENTRAL` | obligatoria | Clave con la que el central consulta pesajes |
| `NODO_CLAVE_ADMIN` | obligatoria | Clave del administrador del nodo |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `balanzas` | Base del nodo |
| `DB_USER` / `DB_PASSWORD` | `balanzas` / `balanzas` | Usuario de la base |

Las claves no van en el repositorio: se pasan como variables de entorno al desplegar.

### Crear un peaje

    curl -X POST -H "Authorization: Bearer <clave-admin>" -H "Content-Type: application/json" \
      -d '{"codigo":"R5-30","nombre":"Peaje Ruta 5 km 30","ruta":"5","km":30,"latitud":-34.62,"longitud":-56.25}' \
      http://localhost:8082/nodo-balanzas/api/peajes

`codigo`, `nombre` y `ruta` son obligatorios, el km no puede ser negativo y el código no se puede repetir.

### Agregarle una balanza

    curl -X POST -H "Authorization: Bearer <clave-admin>" -H "Content-Type: application/json" \
      -d '{"codigo":"balanza-r5-km30","sentido":"HACIA_MONTEVIDEO"}' \
      http://localhost:8082/nodo-balanzas/api/peajes/R5-30/balanzas

`sentido` es `HACIA_MONTEVIDEO` o `HACIA_EL_INTERIOR`. El código de la balanza no se puede repetir en el nodo.
La respuesta trae la clave que el nodo generó para esa balanza:

    {"clave":"Xq3...","codigo":"balanza-r5-km30","sentido":"HACIA_MONTEVIDEO"}

**Se muestra una sola vez**: el nodo guarda solo su hash. Esa clave se configura en el equipo de la balanza
(en la simulación, en el simulador de balanzas).

Para ver los peajes con sus balanzas:

    curl -H "Authorization: Bearer <clave-admin>" http://localhost:8082/nodo-balanzas/api/peajes

### Enviar pesajes (lo hace la balanza)

La balanza manda una o varias lecturas juntas, por ejemplo las que acumuló mientras estuvo sin conexión:

    curl -X POST -H "Authorization: Bearer <clave-de-la-balanza>" -H "Content-Type: application/json" \
      -d '[{"idRegistro":"1001","matricula":"SCC9012","fechaHora":"2026-10-20T09:10:00","pesoRegistrado":36500}]' \
      http://localhost:8082/nodo-balanzas/api/pesajes

- La balanza que envía se toma de la clave, no del mensaje: una balanza no puede mandar lecturas a nombre de otra.
- `idRegistro` lo pone la balanza. Si reenvía una lectura que ya mandó, no se guarda dos veces.
- Si una lectura del lote es inválida (sin `idRegistro`, sin matrícula, sin fecha o con peso 0), no se guarda
  ninguna.
- El nodo guarda además cuándo recibió cada lectura.

### Consultar pesajes (lo hace el central)

    curl -H "Authorization: Bearer <clave-central>" \
      "http://localhost:8082/nodo-balanzas/api/pesajes?matricula=SCC9012&fecha=2026-10-20"

Devuelve todos los pesajes de esa matrícula en ese día, con su balanza y su peaje:

    [{"idRegistro":"1001","matricula":"SCC9012","fechaHora":"2026-10-20T09:10:00","pesoRegistrado":36500,
      "balanza":{"codigo":"balanza-r5-km30","sentido":"HACIA_MONTEVIDEO"},
      "peaje":{"codigo":"R5-30","nombre":"Peaje Ruta 5 km 30","ruta":"5","km":30,"latitud":-34.62,"longitud":-56.25}}]

Cada pesaje se identifica por `balanza/idRegistro` (por ejemplo `balanza-r5-km30/1001`). Si no hubo pesajes
ese día devuelve `[]`.

### Agregar un nodo nuevo

Por ejemplo `balanzas-este`, para otro operador. No hay que tocar código ni el compose:

1. Desplegar la misma imagen con su propia base, y con `NODO_CLAVE_ADMIN` y `NODO_CLAVE_CENTRAL` nuevas
   (por ejemplo `openssl rand -base64 32`).
2. Registrarlo en el central como nodo de tipo `BALANZAS`, con su URL, y habilitarlo.
3. Dar de alta sus peajes y balanzas, a mano o con el script y un inventario propio:

       sh scripts/alta.sh https://balanzas-este.../nodo-balanzas <clave-admin> balanzas-este.json claves-este.json

   `claves-este.json` queda con la clave de cada balanza, para configurarla en el equipo.

### Errores

Igual que en el central: `{"codigo": 2005, "mensaje": "Ya existe un peaje con el codigo R5-30."}`, con el
status HTTP según el caso (400 datos inválidos, 401 sin clave, 403 sin permiso, 404 no existe, 409 repetido).
Los códigos y mensajes están en `CodigoError` y `mensajes-error.properties`.

### Cómo ver que está vivo

`GET http://localhost:9992/health/ready` (puerto de administración de WildFly, el 9990 del contenedor): `200` si el WAR está desplegado.

### Tests

    mvn verify
