# Registro de decisiones de arquitectura

Siete decisiones de carga.uy, con su contexto, la decisión adoptada, las alternativas descartadas y las consecuencias.
Cada una conserva el identificador que tiene en la Vista de Decisiones del
[Documento de Arquitectura](informe/vista-decisiones.tex), donde están también la justificación completa y las
decisiones relacionadas. Las consecuencias resumen lo que se observó al construir, desplegar y probar el sistema;
las mediciones citadas están en [performance.md](performance.md).

| ID | Decisión |
| --- | --- |
| [DA02](#da02-broker-de-mensajería-en-su-propio-contenedor) | Broker de mensajería en su propio contenedor |
| [DA03](#da03-sesión-sin-estado-en-el-servidor) | Sesión sin estado en el servidor |
| [DA04](#da04-ingreso-desde-el-móvil-con-authorization-code-pkce-y-dpop) | Ingreso desde el móvil con *authorization code*, PKCE y DPoP |
| [DA07](#da07-posiciones-en-mongodb-con-idempotencia-por-índice-único) | Posiciones en MongoDB con idempotencia por índice único |
| [DA10](#da10-consulta-a-balanzas-al-finalizar-el-viaje) | Consulta a balanzas al finalizar el viaje |
| [DA11](#da11-permisos-aprobados-por-un-funcionario) | Permisos aprobados por un funcionario |
| [DA12](#da12-criterio-de-desvío-de-ruta) | Criterio de desvío de ruta |

## DA02. Broker de mensajería en su propio contenedor

**Contexto.** La recepción de los eventos de tracking tiene que estar desacoplada de su procesamiento mediante
mensajería asincrónica (R4), con la tecnología, la distribución de los mensajes y la política de entrega justificadas.
El componente central escala horizontalmente (AC07) y los nodos de tracking corren en otro proyecto.

**Decisión.** Apache Artemis (Jakarta Messaging) como broker en su propio contenedor, separado de WildFly y con los
mensajes en un volumen. Los nodos publican en la cola `queue_tracking` con TLS, cada uno con un usuario que solo puede
publicar en esa cola. Todas las instancias del central consumen la misma cola y compiten por sus mensajes. La entrega
es al menos una vez, con reentregas espaciadas de 1 a 60 segundos y hasta 20 intentos antes de la cola de mensajes
muertos.

**Alternativas descartadas.** Mantener el broker embebido en el servidor de aplicaciones, que fue la implementación
inicial: cada instancia de WildFly tenía su propia cola y el nodo le enviaba todo a una sola.

**Consecuencias.**
- La ingesta escala con las instancias: en la prueba en la nube, pasar de 1 a 2 instancias multiplicó por 1,9 las
  posiciones procesadas, y de 2 a 4, por 1,4. Cuando el autoescalado apaga una instancia, el broker reentrega sus
  mensajes a otra y ninguno llega a la cola de mensajes muertos.
- Con el central detenido, el nodo sigue publicando y las posiciones esperan en la cola. Con PostgreSQL detenido, las
  reentregas cubren unos 14 minutos sin perder posiciones.
- Como la entrega es al menos una vez y Artemis no deduplica la reentrega de una transacción deshecha, la ingesta
  tiene que ser idempotente (DA07).
- Artemis usa su propio protocolo sobre TCP. En el Developer Sandbox, que solo publica tráfico web, hubo que exponerlo
  con TLS de extremo a extremo e indicar el destino por SNI en el router.
- Es un contenedor más para operar y monitorear: el tablero de observabilidad muestra la cola y los mensajes muertos.
- La cola `queue_alta_empresa`, que viene del práctico y solo usa el cliente de consola, sigue en el broker embebido
  de cada WildFly.

## DA03. Sesión sin estado en el servidor

**Contexto.** El componente central tiene que escalar horizontalmente sin guardar estado local en las instancias
(AC07). El ingreso con Usuario gub.uy necesita conservar `state`, `nonce` y `code_challenge` entre el inicio del ingreso
y el *callback*, que puede llegar a cualquier instancia.

**Decisión.** La sesión es un JWT firmado (HS256) con la biblioteca JJWT, en una cookie *HttpOnly* en la web y como
token *Bearer* en el móvil. El estado del ingreso con gub.uy viaja en una cookie firmada. El secreto se configura por
entorno, con al menos 32 bytes, y es el mismo en todas las instancias. El backoffice emite el mismo JWT después de
validar las credenciales contra el directorio LDAP.

**Alternativas descartadas.** Mantener el estado en la `HttpSession` de la instancia y firmar el JWT con código propio,
que fue la implementación inicial.

**Consecuencias.**
- Cualquier instancia valida cualquier pedido y el *callback* de gub.uy puede llegar a cualquiera: no hace falta
  afinidad de sesión para la API ni para el frontoffice.
- El backoffice en JSF quedaba atado a la instancia que abrió cada pantalla. Para completar la decisión, el estado de
  las pantallas pasó a viajar cifrado en la página, con una clave común a todas las instancias.
- Una sesión emitida sigue valiendo hasta que vence (seis horas en la web): un cambio de roles o un rechazo posterior,
  como el de un menor que entró con la PDI caída, recién se aplica en el siguiente ingreso.
- El secreto y la clave de las pantallas tienen que ser iguales en todas las instancias y se gestionan como Secrets de
  OpenShift, fuera del repositorio.

## DA04. Ingreso desde el móvil con *authorization code*, PKCE y DPoP

**Contexto.** La aplicación móvil autentica al chofer con Usuario gub.uy (CU-01) y necesita un token para llamar a la
API REST (R2). El ingreso se hace en el navegador, que no comparte sus cookies con la aplicación.

**Decisión.** La aplicación abre el ingreso en el navegador (Custom Tabs) con un `code_challenge`, recibe un código de
un solo uso por el esquema `cargauy://ingreso` y lo canjea, con el `code_verifier` y una prueba DPoP firmada con una
clave del Android Keystore, por un token de acceso de 15 minutos y un token de renovación ligado a esa clave.

**Alternativas descartadas.** Devolver el JWT de sesión en el enlace que abre la aplicación
(`cargauy://ingreso#token=…`), que fue la primera implementación.

**Consecuencias.**
- El enlace solo lleva un código de un solo uso, que únicamente puede canjear quien tiene el `code_verifier`.
- Una copia del token de renovación no sirve en otro equipo, porque cada renovación exige una prueba DPoP firmada con la
  clave del dispositivo; por eso no hace falta rotarlo.
- El central guarda un *hash* del token de renovación y la huella de la clave. Un nuevo ingreso revoca los tokens
  anteriores del mismo usuario y dispositivo, y hay una operación para revocarlo explícitamente.
- La aplicación y el central tienen más piezas que con un token en el enlace: el canje, la verificación de DPoP y la
  gestión de los tokens de renovación.

## DA07. Posiciones en MongoDB con idempotencia por índice único

**Contexto.** Las posiciones son el dato de mayor volumen y solo se agregan. Un mismo evento puede llegar más de una
vez (reentregas del broker o reenvíos del nodo) y la cola tiene varios consumidores en paralelo. Un duplicado no debe
alterar el estado del viaje ni generar casos adicionales.

**Decisión.** Una colección normal de MongoDB, con un índice único sobre el par (nodo, identificador del evento) e
índices por viaje y por matrícula con el momento de generación. Un evento que viola el índice único se descarta como
duplicado. Como MongoDB no participa de la transacción del mensaje, cada posición se guarda sin procesar y se marca
como procesada cuando se confirma esa transacción.

**Alternativas descartadas.** Guardar las posiciones en PostgreSQL con una restricción de unicidad; usar una colección
de series temporales de MongoDB, que no admite índices únicos ni modificar una posición después de guardarla.

**Consecuencias.**
- PostgreSQL queda para el modelo transaccional y no crece con las posiciones. En la nube, el límite de la ingesta fue
  la CPU de PostgreSQL, no el almacén de posiciones.
- Descartar un duplicado cuesta una inserción rechazada, sin consultas previas, aunque varias instancias procesen la
  cola a la vez.
- Sin transacciones compartidas con PostgreSQL, hace falta la marca de procesada: si una posición llega de nuevo
  después de un fallo, no se guarda otra vez pero se completan los pasos que siguen al guardado.
- Las posiciones referencian el vehículo, el viaje y el nodo por identificador, sin claves foráneas: la consistencia
  entre las dos bases la mantiene la aplicación.
- Cumple el opcional 10 (base NoSQL para los eventos de posición).

## DA10. Consulta a balanzas al finalizar el viaje

**Contexto.** La regla de exceso de carga necesita los pesajes del vehículo, pero el nodo de balanzas no los envía por
iniciativa propia: la letra define la integración como solicitud y respuesta (R3), por vehículo y fecha, y hay que
definir en qué momento se consulta (CU-11).

**Decisión.** El Motor de Fiscalización consulta los pesajes cuando procesa el fin de un viaje, por el vehículo y cada
fecha del viaje, a todos los nodos en paralelo.

**Alternativas descartadas.** Consultar con cada evento de posición; consultar periódicamente durante el viaje; que el
nodo de balanzas notifique cada pesaje.

**Consecuencias.**
- Alcanza con una consulta por nodo y por fecha, porque cada respuesta trae todas las pesadas del día.
- Si un nodo no responde, la evaluación del exceso queda pendiente: un temporizador la reintenta cada minuto, en todas
  las instancias, y cada viaje lo toma una sola (`FOR UPDATE SKIP LOCKED`). En la nube, con un nodo caído cuatro minutos
  durante la carga completa, los viajes pendientes se evaluaron unos cuatro minutos y medio después de que volviera.
- La prueba en la nube mostró dos efectos de esperar a un nodo caído, que se corrigieron: la consulta retenía su
  conexión a la base hasta agotar el pool, y el corte de Fault Tolerance, que guarda un estado por método, dejaba de
  consultar a todos los nodos. Se libera la transacción durante la consulta y el corte pasó a ser por nodo.
- El exceso se detecta recién al terminar el viaje. Si el chofer nunca registra el fin, no se evalúa, y un vehículo que
  circula sin viaje no se detecta por pesaje: los dos quedan como limitaciones.

## DA11. Permisos aprobados por un funcionario

**Contexto.** El responsable carga el Permiso Nacional de Circulación con su número y período de validez, y los
funcionarios fiscalizan los permisos. Si el permiso habilitara al vehículo apenas se carga, el responsable podría
declarar cualquier fecha de fin y el vehículo quedaría habilitado sin control.

**Decisión.** El permiso tiene estado: pendiente, aprobado o rechazado. El responsable carga el número y la fecha de
inicio; un funcionario lo aprueba fijando la fecha de fin o lo rechaza con un motivo. Solo un permiso aprobado y vigente
habilita al vehículo. Un aprobado no se modifica: para renovarlo se carga uno nuevo.

**Alternativas descartadas.** Que el permiso habilite apenas se carga y se controle después en la fiscalización; que el
responsable cargue todo el período y el funcionario solo apruebe o rechace.

**Consecuencias.**
- La fecha de fin la fija el MTOP, como en la realidad, y queda registrado quién revisó cada permiso y cuándo.
- Un vehículo nuevo no puede recibir viajes hasta que un funcionario aprueba su permiso: agrega un paso al flujo de la
  empresa y trabajo a los funcionarios.
- Las operaciones del responsable reciben solo los datos que puede declarar: la fecha de fin y el estado no se pueden
  fijar desde la API aunque se envíen.
- La regla de vehículo sin habilitación se apoya en el estado del permiso, además de la ITV.

## DA12. Criterio de desvío de ruta

**Contexto.** Hay que detectar cuándo la ruta reportada por el tracking se aparta significativamente del recorrido
esperado entre el origen y el destino de la Guía, con un criterio definido y justificado. La Guía solo declara el origen
y el destino, y obtener el recorrido por las rutas reales exige un servicio de rutas y soporte geoespacial.

**Decisión.** Con cada posición de un viaje en curso, el Motor de Fiscalización calcula la distancia de la posición al
tramo recto entre el origen y el destino de la Guía. Si supera la distancia vigente (norma `DESVIO_RUTA_DISTANCIA`,
5000 m por defecto, parametrizable con fecha de vigencia), registra un caso de desvío con esa posición como evidencia,
uno por viaje.

**Alternativas descartadas.** Recorrido por las rutas reales, obtenido de un servicio de rutas al registrar la Guía y
guardado en PostGIS, con un corredor y un tiempo máximo fuera de él; evaluar el desvío al finalizar el viaje con todas
sus posiciones.

**Consecuencias.**
- Se implementó con lo que ya existía (el origen y el destino de la Guía, la norma parametrizable y el simulador del
  nodo de tracking) y detecta el desvío durante el viaje, sin leer todas las posiciones.
- En recorridos con muchas curvas puede dar falsos positivos, que el funcionario desestima en el descargo; la distancia
  se ajusta desde el backoffice sin redesplegar.
- Con carga, la regla abrió sus casos sin afectar la ingesta: un caso por viaje, aunque el desvío dure muchas
  posiciones.
- PostGIS no se usa, por lo que el opcional 11 quedó fuera. El siguiente paso es guardar el recorrido real como una
  línea en PostGIS y medir contra ella con un tiempo mínimo fuera del corredor; la norma, la deduplicación y la
  evidencia quedan igual.
