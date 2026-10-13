# Correcciones del Documento de Arquitectura

Este archivo reúne todo lo que falta resolver en el Documento de Arquitectura (`docs/informe`), para que lo tengamos a mano todo el equipo. Complementa los pendientes en rojo del documento. Además de esos pendientes, trae:

- las observaciones de revisión que el documento no marca;
- una propuesta para cada decisión;
- las tareas de código que el documento da por hechas.

Las propuestas son un punto de partida para discutir. Cada decisión hay que tomarla y justificarla en el documento con nuestras palabras, contrastándola con la documentación oficial. Los valores numéricos son valores de partida, no datos.

**Estado:**

- **Documento:** el de la PR #43.
- **Código:** el de la rama principal hasta el PR #39, más las PR #40 y #41.
- **Supuestos:** la entrega final se puede hacer en GitHub, y el frontoffice se puede servir desde Vercel.
- **Actualizado:** 13/10/2026.

**Cómo usarlo:**

- Los identificadores D (decisiones importantes) y M (decisiones y tareas menores) no cambian.
- "Obs. N" remite a la [lista de observaciones](#4-observaciones-de-la-revisión).
- Cuando algo se resuelve en el documento, se borra de acá en la misma PR.

## Contenido

1. [Riesgos frente a la letra](#1-riesgos-frente-a-la-letra)
2. [Decisiones importantes](#2-decisiones-importantes)
3. [Decisiones y tareas menores](#3-decisiones-y-tareas-menores)
4. [Observaciones de la revisión](#4-observaciones-de-la-revisión)
5. [Pendientes marcados en rojo](#5-pendientes-marcados-en-rojo)
6. [Alternativas para completar en las decisiones](#6-alternativas-para-completar-en-las-decisiones)
7. [Respuestas a las dudas sobre el documento](#7-respuestas-a-las-dudas-sobre-el-documento)
8. [Tareas en el código](#8-tareas-en-el-código)
9. [Fuentes](#9-fuentes)

## 1. Riesgos frente a la letra

Puntos en los que la solución descrita podría no cumplir con la letra o quedar incompleta.

1. **Escalado horizontal con estado en las instancias.** La sesión del backoffice JSF guarda estado en cada instancia (AC07). La mensajería ya no: el broker está en su propio contenedor. Obs. 3.
2. **Opcional 4 frente a R14.** Falta ver dónde corre el orquestador si el componente central tiene que estar en Elastic Cloud. Obs. 7.
3. **Elementos "a definir" en el diagrama de distribución.** La plataforma de los componentes periféricos (R15) y la del directorio LDAP y la Plataforma de Observabilidad. Obs. 4; Obs. 6.
4. **Trazado distribuido (punto 9 de observabilidad).** AC23 define el flujo (fin del viaje desde el móvil, componente central y consulta al nodo de balanzas), pero para trazarlo hay que instrumentar la aplicación móvil y el nodo de balanzas, que todavía no lo están.
5. **DA10 deja casos sin cubrir.** Un viaje cuyo fin nunca se registra no se evalúa por exceso de carga, y la regla "viaje sin Guía" no puede detectarse por pesaje. Obs. 10.
6. **Fotografía de los incidentes sin diseño.** Obs. 1.
7. **Plazo de descargo: días hábiles o corridos.** Obs. 2.
8. **Registro de decisiones de la entrega final.** Se piden entre cinco y ocho decisiones con sus consecuencias, y el documento tiene diez. Obs. 8.
9. **Conexión del nodo de tracking con TLS y no con HTTPS (R8).** Obs. 5.

## 2. Decisiones importantes

Ordenadas por importancia. La letra las pide definir y justificar explícitamente, o ponen en riesgo que la solución cumpla con la letra.

### D2. Mensajería: reintentos, cola de mensajes muertos y R8

**Por qué importa:** El broker ya está en su propio contenedor, y DA02 describe cómo se reparten los mensajes y cómo se entregan. Falta la política de reintentos: hoy son 10 intentos sin espera y después la cola de mensajes muertos, sin aviso ni forma de reprocesarla. Si MongoDB queda caído más de unos 5 minutos, las posiciones terminan ahí. Además, R8 pide HTTPS y la conexión del nodo usa TLS sobre el protocolo del broker.

**Propuesta:**

- **Reintentos:** espera creciente, por ejemplo desde 1 s, duplicándose hasta un máximo de 1 min, con 15 intentos. Así se cubre una caída de unos 10 minutos sin trancar la cola.
- **Cola de mensajes muertos:** una métrica con la cantidad de mensajes, como la de la cola de tracking; una alerta en Grafana; y un procedimiento para reenviarlos a la cola de tracking desde la consola del broker. Como la ingesta descarta los duplicados, reenviarlos no duplica nada.
- **R8:** consultar en el monitoreo si TLS sobre el protocolo del broker alcanza. Si no, Artemis puede llevar los mensajes sobre HTTP, y con TLS eso es HTTPS, a cambio de algo de rendimiento.
- **DA02:** confirmar que la descripción del reparto y de la entrega es la que quiere el grupo, y escribir su justificación (D10).

*Dónde:* Obs. 5 · DA02 · R4 · R8 · Fila Reintentos y cola de mensajes muertos

### D3. Volumen objetivo de tracking

**Por qué importa:** La letra pide estimar y justificar el volumen, con picos de al menos tres veces. Es la entrada de la prueba de performance y del generador de carga; no sale de la prueba (ver la respuesta sobre AC08). Corresponde al Lote 3.

**Propuesta:** Estimar los vehículos de carga en viaje a la vez en la hora pico, a partir de los datos del parque habilitado (datos abiertos del MTOP, la DNT o el INE) y de un porcentaje en circulación, y dividir por la frecuencia de reporte del GPS. Por ejemplo, 5.000 vehículos que reportan cada 30 s dan unos 167 eventos/s, y el pico de tres veces, unos 500 eventos/s. La cantidad de vehículos hay que sacarla de una fuente.

*Dónde:* AC08 · Fila Volumen

### D5. Servicio de rutas, criterio de desvío y momento de evaluación

**Por qué importa:** La letra pide definir y justificar el criterio de desvío. El recorrido esperado ya se obtiene de un servicio externo de rutas y se guarda en PostGIS; falta elegir el servicio, los valores del criterio y cuándo se evalúa.

**Propuesta:**

- **Servicio:** OpenRouteService, que da geocodificación y recorridos con un plan gratuito de 1.000 geocodificaciones y 2.000 recorridos por día. Con dos geocodificaciones y un recorrido por Guía alcanza.
- **Criterio:** hay desvío con, por ejemplo, 3 posiciones consecutivas a más de 2 km del recorrido, con ambos valores parametrizables desde el backoffice. Exigir varias posiciones evita falsos positivos por imprecisión del GPS.
- **Momento:** evaluar cada viaje en curso cada pocos minutos sobre las posiciones nuevas, y una última vez al finalizar. Evaluar con cada posición daría una consulta a PostGIS por evento; evaluar solo al final llega tarde.

*Dónde:* Filas Servicio de rutas, Criterio de desvío y Momento de evaluación · CU-07 · CU-11

### D6. Umbral mínimo de agregación de la información pública

**Por qué importa:** R12 pide definir y justificar el umbral para que lo publicado no permita identificar a una empresa, un vehículo o un chofer.

**Propuesta:** Publicar un dato agregado solo si agrupa al menos 5 empresas distintas, y aplicar la misma regla a vehículos y choferes. Las celdas con menos se suprimen o se agrupan en "Otros". Justificarlo con la Ley 18.331 y con la práctica de control de divulgación estadística, que usa un mínimo de casos por celda.

*Dónde:* AC06 · R12 · Fila Umbral

### D9. Escalado horizontal y tecnología del backoffice (JSF o React)

**Por qué importa:** AC07 pide escalar sin estado local en las instancias. DA03 sacó de la sesión el estado del ingreso, pero quedan tres beans JSF con `@ViewScoped` (`EmpresaView`, `NodoView` y `VehiculoView`), que JSF guarda en la sesión HTTP de cada instancia.

**Propuesta:** Pasar el backoffice a React: elimina el único estado de sesión del servidor y aprovecha la API y la sesión sin estado que ya existen. El costo es rehacer las pantallas JSF, entre ellas la de nodos. Si el grupo prefiere mantener JSF, documentar la afinidad de sesión en el balanceador y aceptar que, si se cae una réplica, se pierden las pantallas abiertas en ella.

*Dónde:* Obs. 3 · Obs. 9 · AC07 · Filas Escalado y Tecnología del backoffice

### D10. Alternativas y justificación de las decisiones, y registro de decisiones

**Por qué importa:** Las alternativas y justificaciones de DA01, DA02, DA04, DA05, DA06, DA08 y DA09 son 14 de las 25 marcas en rojo. Además, la entrega final pide un registro de 5 a 8 decisiones con contexto, alternativas descartadas y consecuencias; el documento tiene 10 y la plantilla no tiene campo de consecuencias.

**Propuesta:** Primero, pasar a la Vista de Restricciones lo que no se eligió, como DA01 si la impone el práctico (ver la respuesta sobre decisiones de la letra). Después, armar el registro de la entrega final con las 8 decisiones que más pesan: mensajería (DA02), sesión sin estado (DA03), ubicación del frontoffice (DA05), posiciones en MongoDB (DA07), consulta a balanzas (DA10), resiliencia (AC13), observabilidad con OpenTelemetry y conflictos del móvil (AC20). Las alternativas están en la tabla de alternativas. La justificación la escribe el grupo.

*Dónde:* Obs. 8 · DA01 a DA09

### D11. Orquestador del opcional 4, Escenario 2 y prueba de performance

**Por qué importa:** El grupo comprometió el despliegue del componente central en un orquestador con escalado horizontal, pero R14 exige que el componente central corra en Elastic Cloud. El Escenario 2 tiene que describir ese despliegue, que sirve también para la prueba de carga.

**Propuesta:**

- Verificar si Elastic Cloud ofrece un orquestador. Si no lo ofrece, consultar en el monitoreo si se puede demostrar el escalado en otro, por ejemplo el OpenShift que el grupo ya usa, mientras producción sigue en Elastic Cloud.
- Describirlo como Escenario 2: orquestador, réplicas del componente central y balanceador.
- Con varias réplicas, la sesión del backoffice JSF (D9) tiene que estar resuelta antes de la prueba. La ingesta ya escala: las réplicas comparten el broker.
- Herramientas de la prueba: k6 para las consultas del frontoffice, donde se mide el percentil 95 menor a 500 ms, y el nodo de tracking en modo carga para la ingesta. La evidencia sale del tablero de Grafana, que ya está armado, bajando la proporción de trazas con `OTEL_PROPORCION_TRAZAS`.

*Dónde:* Obs. 7 · §7.2 · R14 · AC07 · Filas Escalado y Prueba de performance

## 3. Decisiones y tareas menores

También hay que decidirlas, pero son acotadas.

### M1. Plataforma de los componentes periféricos, LDAP y observabilidad

**Propuesta:** créditos de estudiante de una nube que no suspenda los servicios, por ejemplo Azure o DigitalOcean a través del GitHub Student Developer Pack, que la letra referencia. Ahí pueden correr los nodos, el suplantador de la PDI, el directorio LDAP y la Plataforma de Observabilidad. Evitar los planes gratuitos que apagan el servicio por inactividad. Verificar la oferta vigente.

*Dónde:* Obs. 4 · Obs. 6 · §7.1 · R15 · Fila Plataforma de los periféricos

### M5. Fotografías de los incidentes

**Propuesta:** guardarlas en PostgreSQL, en una tabla aparte, comprimidas en el celular (por ejemplo, JPEG con unos 1600 px del lado mayor). El evento se sincroniza primero y la foto se sube después, con su propio identificador y los reintentos de WorkManager.

*Dónde:* Obs. 1 · Fila Fotografías

### M6. Plazo de descargo: días hábiles o corridos

**Propuesta:** el #41 ya lo deja como parámetro con fecha de vigencia (10 días por defecto) y resuelve el vencimiento: un caso notificado sin descargo lo resuelve el funcionario recién vencido el plazo. Falta decidir si se cuenta en días hábiles, que es lo habitual para evacuar una vista (verificarlo en el Decreto 500/991). En ese caso hace falta un calendario de feriados, que puede ser una tabla más de las reglas.

*Dónde:* Obs. 2 · Fila Plazo de descargo

### M7. Casos que DA10 no cubre

**Propuesta:** una tarea programada que evalúe igual un viaje que sigue en curso pasado un margen, por ejemplo 24 h sin posiciones después de la fecha de la Guía. Documentar que la regla "viaje sin Guía" por pesaje solo aplica a vehículos ya sospechados, porque balanzas responde por vehículo y fecha. Agregar ambas cosas como consecuencias de DA10.

*Dónde:* Obs. 10 · Fila Viaje sin fin registrado

### M13. Requerimientos que justifican el Escenario 1

**Propuesta:** apoyarse en R14, R15, R8, AC07, AC12 y AC18 (ver la respuesta sobre escenarios). Se puede redactar una vez decididos D2 y M1.

*Dónde:* §7.1

### M14. Tecnologías, pros y contras, y habilidades

**Propuesta:** una tabla corta: qué impone el curso y qué eligió el grupo, una ventaja y una desventaja de lo elegido y las habilidades que el proyecto exigió. Ver la respuesta sobre tecnologías.

*Dónde:* §8.1

### M15. Refinamientos adicionales de la vista lógica

**Propuesta:** con un refinamiento alcanza. Si sobra tiempo, el del Motor de Fiscalización; si no se agrega, quitar el pendiente.

*Dónde:* §6.2

### M16. Valores de AC13 para el servicio de rutas y el directorio LDAP

**Propuesta:** para el servicio de rutas, los mismos valores que balanzas (espera de 5 s, 2 reintentos a 1 s y 2 s, y corte por 60 s tras 5 fallas en 10 llamadas), porque registrar una Guía no es urgente y el recorrido puede quedar pendiente. Para LDAP, los mismos que Usuario gub.uy (espera de 5 s, sin reintentos), porque es un ingreso interactivo.

*Dónde:* AC13

### M18. Tecnología del nodo de balanzas, del suplantador de la PDI y del directorio LDAP

**Propuesta:** el nodo de balanzas y el suplantador de la PDI con el mismo stack que el componente central (Jakarta EE: JAX-RS para balanzas y JAX-WS para el SOAP de la PDI), porque el grupo ya lo conoce. Para el directorio, OpenLDAP en un contenedor.

*Dónde:* §8.1

## 4. Observaciones de la revisión

Cosas que conviene corregir o completar en el documento y que no siempre están marcadas en rojo.

### Obs. 1. Fotografía de los incidentes

*Dónde:* §3.3 Funcionalidades obligatorias, componente móvil (operación sin conectividad) · *Ítem:* M5

Ninguna vista define dónde se almacena la fotografía de un incidente, cómo se envía desde el móvil al recuperar la conexión ni qué tamaño se admite. El registro de eventos del móvil todavía no la contempla.

### Obs. 2. Plazo de descargo

*Dónde:* §3.6 CU-11, después de "Consulta a balanzas" · *Ítem:* M6

El plazo ya es un parámetro con fecha de vigencia (10 días por defecto), y un caso notificado sin descargo se resuelve recién vencido el plazo. Falta definir si se cuenta en días hábiles o corridos; con días hábiles hace falta un calendario de feriados.

### Obs. 3. AC07: escalado horizontal

*Dónde:* §5 Vista de Atributos de Calidad, AC07 · *Ítem:* D9, D11

La mensajería ya no guarda estado en las instancias: el broker está en su propio contenedor y lo comparten todas (DA02). Queda el backoffice JSF: sus beans `@ViewScoped` viven en la sesión HTTP de cada instancia. Definir cómo escala con ellos (por ejemplo, afinidad de sesión, replicación de la sesión o pasar el backoffice a React) y reflejarlo en la vista de distribución.

### Obs. 4. Elementos "a definir" en el diagrama

*Dónde:* §7.1 Escenario 1, diagrama de distribución · *Ítem:* M1

El diagrama todavía tiene elementos "a definir": la plataforma de los componentes periféricos y la del Directorio LDAP y la Plataforma de Observabilidad. Hay que quitarlos una vez tomadas esas decisiones (Obs. 6). Si al momento de entregar alguna sigue abierta, es preferible indicar las opciones evaluadas y el criterio para elegir.

### Obs. 5. R8 y la conexión del nodo de tracking

*Dónde:* §7.1 Escenario 1, primer párrafo · *Ítem:* D2

R8 pide que las interacciones del componente central con los componentes periféricos usen HTTPS. La conexión del nodo de tracking con el broker usa el protocolo del broker cifrado con TLS: protege la comunicación igual, pero no es HTTPS en sentido literal. Consultarlo en el monitoreo. Si no lo acepta, Artemis puede transportar sus mensajes sobre HTTP, y con TLS eso es HTTPS.

### Obs. 6. Plataforma de los componentes periféricos

*Dónde:* §7.1 Escenario 1, "PaaS o IaaS periférica" · *Ítem:* M1

El texto no nombra la plataforma y el diagrama la rotula "plataforma a definir". R15 exige ejecutar los componentes periféricos en otra solución PaaS o IaaS con cuenta gratuita: indicar cuál se usa para los nodos de balanzas y de tracking y para el suplantador de la PDI, y reflejarlo en el diagrama y en la vista de implementación.

### Obs. 7. Escenario 2 y R14

*Dónde:* §7.2 Escenario 2 · *Ítem:* D11

El orquestador del opcional 4 se tiene que compatibilizar con R14, que exige ejecutar el componente central en Elastic Cloud: verificar si Elastic Cloud ofrece un orquestador o consultarlo en el monitoreo. Con varias réplicas, la sesión del backoffice JSF pasa a ser un problema concreto (Obs. 3); la ingesta ya escala, porque las réplicas comparten el broker. Indicar también con qué herramienta se ejecuta la prueba de performance.

### Obs. 8. Registro de decisiones de la entrega final

*Dónde:* §9 Vista de Decisiones, introducción · *Ítem:* D10

La entrega final pide un registro de entre cinco y ocho decisiones relevantes, cada una con su contexto, la decisión adoptada, las alternativas descartadas y las consecuencias. Esta vista tiene diez decisiones y la plantilla no incluye un campo de consecuencias. Definir si esta vista cumple ese papel (y en ese caso reducirla a ocho decisiones y agregar las consecuencias) o si el registro se entrega por separado.

### Obs. 9. DA05

*Dónde:* §9 DA05 · *Ítem:* D9

Definir si el backoffice queda en JSF o pasa a React: hoy hay páginas JSF de empresas y vehículos que duplican el frontend, y los beans `@ViewScoped` afectan el escalado (Obs. 3).

### Obs. 10. DA10: consecuencias

*Dónde:* §9 DA10 · *Ítem:* M7

Faltan las consecuencias de la decisión. (a) El exceso de carga se detecta recién cuando el viaje termina. (b) Si el chofer nunca registra el fin del viaje, los pesajes no se consultan y el exceso de carga no se evalúa; definir un mecanismo alternativo, por ejemplo un cierre por plazo. (c) A un vehículo sin viaje nunca se le consultan los pesajes, así que la regla "viaje sin Guía" solo puede detectarse por tracking, aunque la letra la define también para la información de pesaje. Explicitarlo o definir cómo se cubre ese caso.

## 5. Pendientes marcados en rojo

Los 25 pendientes que el documento marca en rojo y el ítem de este archivo que los resuelve.

| Sección | Pendiente | Ítem |
|---|---|---|
| §5 AC06 | Definir y justificar el umbral de agregación (R12) | D6 |
| §5 AC08 | Estimar y justificar el volumen: vehículos reportando a la vez y frecuencia de reporte | D3 |
| §5 AC13 | Servicio de rutas: definir la espera, los reintentos y el corte | M16 |
| §5 AC13 | Directorio LDAP: definir la espera, los reintentos y el corte | M16 |
| §6.2 | Agregar los refinamientos de otros subsistemas que hagan falta | M15 |
| §7.1 | Definir en qué plataforma se ejecutan el Directorio LDAP y la Plataforma de Observabilidad | M1 |
| §7.1 | Describir los requerimientos no funcionales que justifican el escenario | M13 |
| §7.2 | Describir el despliegue en un orquestador de contenedores (opcional 4), compatible con R14 | D11 |
| §8.1 | Definir la tecnología del nodo de balanzas, del suplantador de la PDI y del directorio LDAP | M18 |
| §8.1 | Describir pros y contras de las tecnologías y las habilidades necesarias | M14 |
| §9 DA01, DA02, DA04, DA05, DA06, DA08 y DA09 (14) | Completar las alternativas consideradas y la justificación | D10 |
| §9 DA02 | Definir la espera entre reintentos, la cantidad de intentos y qué se hace con la cola de mensajes muertos | D2 |

La tabla "Decisiones pendientes" del documento (§9.1) tiene 14 filas:

| Fila | Ítem |
|---|---|
| Volumen de tracking | D3 |
| Reintentos y cola de mensajes muertos | D2 |
| Servicio de rutas y geocodificación · Criterio de desvío de ruta · Momento de evaluación del desvío | D5 |
| Umbral de agregación | D6 |
| Escalado y orquestador · Prueba de performance | D11 |
| Tecnología del backoffice | D9 |
| Plataforma de los periféricos · Plataforma de observabilidad | M1 |
| Fotografías de los incidentes | M5 |
| Plazo de descargo | M6 |
| Viaje sin fin registrado | M7 |

## 6. Alternativas para completar en las decisiones

Decisiones con la decisión tomada pero sin alternativas ni justificación. Las que se impusieron desde afuera pueden pasar a Restricciones. La justificación la escribe el grupo.

| Decisión | Origen | Alternativas a considerar |
|---|---|---|
| DA01 Capas | Práctico | Si la impone el práctico, pasarla a Restricciones. Si no: arquitectura hexagonal, monolito modular por dominio o microservicios. |
| DA02 Mensajería | Elección (R4) | Kafka; RabbitMQ; Artemis embebido en WildFly, que fue la primera implementación. La letra pide justificar la elección. |
| DA04 PKCE | Estándar | Las que descarta el estándar (RFC 8252): flujo implícito, secreto embebido en la app, ingreso en una WebView. Citar el estándar alcanza como justificación. |
| DA05 Presentación | Letra y elección | Angular, Vue o JSF, justificando React por el conocimiento del equipo; para la ubicación, Elastic Cloud frente a Vercel. |
| DA06 Móvil | Elección | Flutter; React Native; aplicación web progresiva; Kotlin Multiplatform. |
| DA08 Nodos | Letra y elección | La gestión la pide la letra; el diseño es del grupo. Alternativas: lista fija en la configuración o registro sin estados. |
| DA09 Autorización | Elección (R9) | Verificaciones programáticas en cada método; políticas de Jakarta Authorization. El control solo en la presentación no cumple R9. |

## 7. Respuestas a las dudas sobre el documento

Las dudas que surgieron en la conversación del equipo, con lo que dicen la plantilla y la letra.

**Duda:** ¿Hay que poner alternativas en decisiones que vinieron de la letra o del práctico? DA01 viene del práctico, DA02 casi también, DA04 es el estándar de la industria, DA05 es de letra y React es lo que conocemos, DA08 también es de letra.

La plantilla dice que la Vista de Decisiones presenta las decisiones tomadas "sopesando las diferentes alternativas que se presentaban al momento de la decisión". En la Vista de Restricciones pone como ejemplos de restricción las "tecnologías o frameworks existentes que deben ser utilizados" y la "habilidad de los desarrolladores". Entonces, lo que no se eligió es una restricción, no una decisión.

**Propuesta:**

- **DA01:** si la arquitectura en capas la impone el práctico, pasarla a la Vista de Restricciones (por ejemplo, como R18) y sacarla de las decisiones. Además ayuda a no pasar de 8 en el registro de decisiones.
- **DA02:** no es de letra. R4 exige mensajería, pero pide explícitamente "justificar la elección realizada, la estrategia de particionamiento o distribución de los mensajes y la política de entrega", y la propia letra da Kafka y RabbitMQ como opciones. Es la decisión que más necesita alternativas.
- **DA04:** que sea el estándar es la justificación. Alcanza con citarlo (RFC 8252, que recomienda *authorization code* con PKCE para aplicaciones nativas) y nombrar lo que el estándar descarta: el flujo implícito, un secreto embebido en la app y el ingreso dentro de una WebView.
- **DA05:** la letra exige frontoffice y backoffice web, pero no React. "Es lo que conocemos" es una justificación válida: la plantilla cuenta la habilidad de los desarrolladores como un factor. Alternativas: Angular, Vue o JSF. Vercel ya está habilitado para el frontoffice.
- **DA08:** la letra pide que los administradores gestionen los nodos, pero los estados, el alta deshabilitada y la baja lógica son elecciones del grupo. Ahí sí hay alternativas, aunque sean breves: una lista fija en la configuración o un registro sin estados.

---

**Duda:** En componentes ejecutables, ¿qué hay que poner? Jakarta EE 10 es requisito, y lo de habilidades necesarias no sé qué poner; no sé si esperan que vendamos humo.

La plantilla, al describir la Vista de Implementación, dice que se detallan "las opciones tecnológicas (e.g. si se utiliza Java o Node.js) junto con los pros y los contras de su uso" y que "se deberían justificar los recursos y las habilidades necesarias para ejecutar el proyecto". La letra no nombra Jakarta EE; la impone el curso.

**Propuesta:**

Una tabla corta y honesta, sin humo:
- Para cada tecnología, indicar si la impone el curso o la letra (Jakarta EE y WildFly) o si la eligió el grupo (React, Kotlin, PostgreSQL con PostGIS, MongoDB, Artemis).
- Para las elegidas, una ventaja y una desventaja concretas.
- Habilidades: las que el proyecto exigió de verdad, por ejemplo EJB, JPA y JMS; React con TypeScript; Android con Kotlin; Docker. Indicar cuáles ya tenía el grupo y cuáles hubo que aprender.
- Recursos: los cuatro integrantes, los códigos de Elastic Cloud y las cuentas gratuitas de las plataformas periféricas.

---

**Duda:** En los escenarios no sé qué poner ni de dónde salen.

La plantilla define la distribución como "cómo va a funcionar el sistema en el entorno productivo", con sus servidores, balanceadores y bases de datos, y pide, para cada escenario, los requerimientos no funcionales que lo justifican. Un escenario es una forma de desplegar la solución. Los requerimientos que la justifican salen de la letra.

**Propuesta:**

- **Escenario 1:** el despliegue final, que es el que ya está dibujado. Se justifica con R14 (el central en Elastic Cloud), R15 (los periféricos en otra PaaS), R8 (HTTPS entre entornos), AC07 (escalado del central), AC12 (el central sigue funcionando aunque caiga un periférico) y AC18 (móvil sin conexión).
- **Escenario 2:** el despliegue del componente central en el orquestador del opcional 4, con varias réplicas, que también sirve para la prueba de carga (D11).

---

**Duda:** AC08 (volumen) es medio a ensayo y error.

Son dos cosas distintas. AC08 pide el volumen que la plataforma *debe* soportar: un estimado que sale del problema (cuántos vehículos reportan a la vez y cada cuánto), con picos de tres veces. Es una entrada de la prueba de performance y del generador de carga. Lo que sí sale de probar es cuánto aguanta el sistema de verdad, el punto de quiebre de AC11.

**Propuesta:** Estimar el volumen antes de la prueba, con una fuente para la cantidad de vehículos (ver D3). Después, la prueba muestra si el sistema lo sostiene y dónde se quiebra.

---

**Duda:** No sé si pondría más refinamientos.

La plantilla trae "Refinamiento 1 … Refinamiento N" como ejemplo, sin un mínimo.

**Propuesta:** Con un refinamiento alcanza. Si sobra tiempo, el que más aporta es el del Motor de Fiscalización, porque realiza el CU-11. Quitar el pendiente si se decide no agregar más (M15).

## 8. Tareas en el código

No son correcciones del documento, pero el documento las da por hechas en la entrega final.

- **PostGIS, LDAP y servicio de rutas:** imagen de PostgreSQL con PostGIS, un directorio LDAP con su configuración en WildFly y el adaptador del servicio de rutas con su clave fuera del repositorio.
- **MicroProfile Fault Tolerance (AC13):** no viene activado en la configuración por defecto de WildFly; el Dockerfile tiene que agregar su extensión y su subsistema.
- **Credenciales fuera del repositorio (R11):** el usuario de WildFly que crea el Dockerfile, las claves por defecto del Docker Compose (PostgreSQL y MongoDB), los usuarios del broker y su certificado TLS. Quedan en el repositorio hasta el final, a propósito; antes de la entrega se sacan y se cambian.
- **Entorno completo con un solo comando (R13):** sumar al Docker Compose el frontend en un contenedor con su build estático (en producción lo sirve Vercel), el directorio LDAP, el nodo de balanzas y el suplantador de la PDI. MongoDB, el broker y la Plataforma de Observabilidad ya están.
- **Validaciones del CU-07:** la asignación del viaje ya controla la capacidad, el Permiso vigente y que el chofer esté vinculado a la empresa en la fecha de la Guía, pero todavía no controla la ITV vigente ni los viajes solapados, que el documento describe.
- **Trazado distribuido del punto 9 (AC23):** el flujo es el fin del viaje desde el móvil, el componente central y la consulta al nodo de balanzas. El central ya está instrumentado; falta que la app móvil mande el contexto de la traza (`traceparent`) en sus pedidos y que el nodo de balanzas lo continúe.
- **Aviso al Motor de Fiscalización desde la ingesta (CU-11):** queda pendiente hasta que exista el Motor. Va donde la ingesta marca los pasos que siguen al guardado, y el Motor tiene que ignorar los avisos repetidos.
- **Recorridos del generador de carga:** los camiones simulados van en línea recta y el recorrido esperado de la Guía sigue las rutas reales, así que el Motor vería desvíos que no existen. Queda pendiente hasta elegir el servicio de rutas (D5).
- **Imágenes en el CI (#40):** el paso que construye las imágenes hace las del central y del nodo de tracking. Faltan las del broker (`carga-uy/artemis`) y de la Plataforma de Observabilidad (`carga-uy/observabilidad`), que entraron después.
- **Notificación por correo (CU-11):** el #41 notifica por la bandeja web, de forma provisoria. Falta el envío por correo, y que el caso pase a notificado recién cuando se confirme el envío, como dice el documento.

## 9. Fuentes

Documentación oficial contra la que conviene contrastar cada propuesta antes de decidir.

- Letra del laboratorio 2026 y plantilla del documento de arquitectura del curso (descripción de las vistas).
- [WildFly: quickstart de MicroProfile Fault Tolerance](https://docs.wildfly.org/quickstart/microprofile-fault-tolerance/README.html)
- [WildFly: subsistema microprofile-fault-tolerance-smallrye](https://docs.wildfly.org/36/wildscribe/subsystem/microprofile-fault-tolerance-smallrye/index.html)
- [WildFly: subsistema de OpenTelemetry](https://docs.wildfly.org/31/wildscribe/subsystem/opentelemetry/index.html)
- [OpenTelemetry: exportador OTLP](https://opentelemetry.io/docs/specs/otel/protocol/exporter/)
- [ActiveMQ Artemis: configuración de direcciones (reintentos, cola de mensajes muertos, paginación)](https://activemq.apache.org/components/artemis/documentation/latest/address-settings.html)
- [ActiveMQ Artemis: agrupamiento de mensajes](https://activemq.apache.org/components/artemis/documentation/latest/message-grouping.html)
- [RFC 7636: PKCE](https://www.rfc-editor.org/rfc/rfc7636) y [RFC 8252: OAuth 2.0 para aplicaciones nativas](https://www.rfc-editor.org/rfc/rfc8252)
- [OSRM: motor de rutas sobre OpenStreetMap](https://project-osrm.org/)
- [OpenRouteService: planes y límites](https://openrouteservice.org/plans/) y [Nominatim (OpenStreetMap)](https://wiki.openstreetmap.org/wiki/Nominatim)
- [Grafana: imagen grafana/otel-lgtm](https://grafana.com/blog/an-opentelemetry-backend-in-a-docker-image-introducing-grafana-otel-lgtm) y [Jaeger: primeros pasos](https://jaegertracing.io/docs/2.0/getting-started)
- [Ley 18.331 de protección de datos personales](https://www.impo.com.uy/bases/leyes/18331-2008) y [Decreto 500/991](https://www.impo.com.uy/bases/decretos/500-1991) (IMPO)
- [GitHub Student Developer Pack](https://education.github.com/pack)
