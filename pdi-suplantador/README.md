# Suplantador de la PDI

[Volver al proyecto](../README.md)

Simula el **Servicio Básico de Información** de DNIC, que en la realidad se consume por la Plataforma de
Interoperabilidad (PDI). Los servicios de la PDI no son públicos, así que la letra pide suplantarlos respetando su
interfaz en lo posible. El central lo usa en el ingreso de un ciudadano, para obtener su nombre y su fecha de
nacimiento.

Es un WAR de Jakarta EE sobre WildFly 41 con JAX-WS. **No tiene base ni datos reales:** las personas son ficticias y
están en [personas.json](personas/personas.json), salvo los [casos especiales](#casos-especiales).

## Qué tan fiel es al servicio real

| Aspecto | Real | Suplantador |
| --- | --- | --- |
| Operaciones, campos y códigos | [Descripción funcional de AGESIC](https://www.gub.uy/agencia-gobierno-electronico-sociedad-informacion-conocimiento/sites/agencia-gobierno-electronico-sociedad-informacion-conocimiento/files/2020-01/Documentacion%20funcional-DNIC%20servicio%20basico%20info.pdf) | Los mismos |
| Transporte | SOAP sobre HTTPS | SOAP sobre HTTP en el Compose; falta HTTPS (ver [despliegue](#despliegue)) |
| Errores | En el cuerpo de la respuesta | Igual |
| WSDL, namespace, `soapAction` | No son públicos | Armados por el grupo a partir de la descripción funcional |
| WS-Addressing y token SAML por WS-Security | Los exige la PDI | No se simulan (es el opcional 9) |

El contrato está en [ServicioBasicoInformacion.wsdl](src/main/webapp/WEB-INF/wsdl/ServicioBasicoInformacion.wsdl). El
central guarda su propia copia en `carga-uy/carga-uy-ejb/src/main/resources/wsdl/pdi/`, como haría un consumidor real:
si cambia el contrato, hay que actualizar las dos.

## Operaciones

### ObtPersonaPorDoc

Dada una cédula, sin puntos ni guión, devuelve los datos filiatorios.

    curl -s -H "Content-Type: text/xml; charset=utf-8" \
      -d '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:dnic="urn:carga-uy:suplantador-pdi:dnic:servicio-basico-informacion"><soapenv:Body><dnic:paramobtPersonaPorDoc><dnic:organizacion>carga-uy</dnic:organizacion><dnic:passwordEntidad>clave-pdi-desarrollo</dnic:passwordEntidad><dnic:NroDocumento>55555555</dnic:NroDocumento><dnic:TipoDocumento>DO</dnic:TipoDocumento></dnic:paramobtPersonaPorDoc></soapenv:Body></soapenv:Envelope>' \
      http://localhost:8085/pdi-suplantador/ServicioBasicoInformacion

La respuesta trae `objPersona`, y `Warnings` o `Errores` con `CodMensaje`, `Descripcion` y `DatoExtra`. La fecha va en
el formato de DNIC, `yyyy-mmdd`, y puede venir vacía o solo con año-mes o año.

### ProductDesc

No tiene parámetros. Devuelve `version`, `modalidad` (`Testing`) y `descripcion`.

## Personas

Todas las personas con datos están en [personas.json](personas/personas.json), por ejemplo:
- Los ciudadanos de los datos de prueba: `55555555`, el chofer Carlos, y `22222222`, la responsable Ana.
- Las cédulas que usan los tests.
- `44444444`, que tiene una fecha parcial (`1985`).

Para sumar una, ver [Agregar personas](#agregar-personas).

## Casos especiales

Simulan comportamientos del servicio que no se pueden escribir como datos fijos, así que están en
[CasosEspeciales](src/main/java/tse/cargauy/pdisuplantador/CasosEspeciales.java). Tienen prioridad sobre el archivo.

| Cédula | Qué responde |
| --- | --- |
| `33333333` | Lucía Fernández, menor de edad: la fecha es siempre hoy menos 16 años, y el central rechaza su ingreso |
| `66666666` | Marta Núñez, con la advertencia 701, "Datos de Persona a regularizar" |
| `77777777` | Error 1003, "Número de cédula anulado" |
| `88888888` | Error 1001, "No se pudo completar la consulta" (falla transitoria) |

Una cédula que no está en ningún lado responde el error 500, "Persona inexistente". Además, 10001 si los parámetros
son incorrectos y 10002 si `organizacion` o `passwordEntidad` no coinciden con las configuradas.

## Agregar personas

Para sumar personas sin reconstruir la imagen, hay que agregarlas a [personas.json](personas/personas.json). El
suplantador lee el archivo en cada consulta, así que el cambio se ve en la siguiente, sin reiniciar. **Solo personas
ficticias:** el archivo está en Git.

| Campo | Obligatorio | Formato |
| --- | --- | --- |
| `cedula` | Sí | De 6 a 8 dígitos, sin puntos ni guión |
| `nombre1`, `apellido1` | Sí | |
| `nombre2`, `apellido2` | No | |
| `sexo` | No | `1` masculino, `2` femenino |
| `fechaNacimiento` | No | `yyyy-mmdd`, `yyyy-mm` o `yyyy` |

- Las cédulas de los [casos especiales](#casos-especiales) siempre responden lo suyo: si también están en el archivo,
  esa entrada se ignora.
- Si el archivo tiene un error, se ignora completo y solo responden los casos especiales. El motivo se ve con
  `docker compose logs pdi-suplantador`.
- Para no llegar a eso, `mvn verify` y la construcción de la imagen fallan si el archivo del repo no es válido o si le
  faltan Carlos o Ana.

El central solo consulta a la PDI si al ciudadano le faltan los datos. Para cambiar a alguien que ya los tiene, hay que
vaciar su perfil desde `carga-uy` y volver a ingresar:

    docker compose exec -T postgres psql -U cargauy -d cargauy -c "UPDATE ciudadano SET nombres=NULL, apellidos=NULL, fechanacimiento=NULL WHERE cedula='55555555'"

## Cómo levantarlo

El [Compose](../carga-uy/docker-compose.yml) ya lo incluye. Queda en el puerto `8085`, y el contrato se puede ver en
http://localhost:8085/pdi-suplantador/ServicioBasicoInformacion?wsdl.

## Probar la resiliencia del central

El central aplica una espera de 3 s, un reintento a 1 s y un corte de 30 s tras 5 fallas en 10 llamadas (AC13).

| Escenario | Cómo | Qué tiene que pasar |
| --- | --- | --- |
| PDI caída | `docker compose stop pdi-suplantador` | El ciudadano entra igual, sin los datos; se completan en su próximo ingreso con la PDI disponible |
| PDI que no responde | `PDI_DEMORA_MS=10000 docker compose up -d pdi-suplantador` | Con el perfil pendiente, el ingreso tarda unos 7 s (dos esperas de 3 s y el reintento). Con el corte abierto, que puede tardar hasta 5 ingresos en abrirse, es inmediato |
| PDI que falla | `PDI_FALLA=true docker compose up -d pdi-suplantador` | Lo mismo que caída: el error 1001 cuenta como falla |

## Configuración

| Variable | Por defecto | Para qué |
| --- | --- | --- |
| `PDI_ORGANIZACION` | obligatoria | Organización que acepta |
| `PDI_PASSWORD_ENTIDAD` | obligatoria | Clave que acepta; sin ella rechaza todo con 10002 |
| `PDI_PERSONAS_ARCHIVO` | `/config/personas/personas.json` | Ruta del [archivo de personas](#agregar-personas) dentro del contenedor. La imagen trae el del repo, y el Compose monta encima la carpeta `personas/` |
| `PDI_DEMORA_MS` | `0` | Cuánto tarda en responder |
| `PDI_FALLA` | `false` | Responder 1001 a todo |

El Compose incluye valores predeterminados para desarrollo. Para otro despliegue, configurarlos mediante variables de
entorno.

## Despliegue

Por ahora no se despliega: solo corre en el Compose, por HTTP, igual que los nodos de balanzas. El HTTPS que pide la
letra hacia las plataformas externas queda para cuando se despliegue. Sin `PDI_URL`, el central deja entrar a los
ciudadanos sin los datos de la PDI y sin control de mayoría de edad.

## Tests

    mvn verify
