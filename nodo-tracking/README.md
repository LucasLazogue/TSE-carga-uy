## Nodo periférico de tracking (simulado)

Genera posiciones de vehículos y las publica en la cola `queue_tracking` del componente central
(CU-10). Entra por el puerto web de WildFly, con el cliente JMS remoto. Sirve para probar la ingesta:
puede reenviar eventos y mandarlos desordenados.

Requiere el componente central levantado (`docker compose up` en `carga-uy`) y el nodo registrado y
habilitado (CU-19):

    curl -X POST -H 'Content-Type: application/json' \
      -d '{"identificador":"tracking-sim","tipo":"TRACKING","puntoAcceso":"https://tracking.example"}' \
      http://localhost:8080/carga-uy/api/nodos
    curl -X POST http://localhost:8080/carga-uy/api/nodos/1/habilitar

También se puede hacer desde el backoffice: http://localhost:8080/carga-uy/nodos.xhtml

Las credenciales JMS se pasan por entorno (en local son las que crea el Dockerfile):

    NODO_JMS_USUARIO=admin NODO_JMS_CLAVE=admin123 mvn compile exec:java \
      -Dexec.args="--eventos 50 --duplicados 0.5 --desordenar"

Opciones: `--url` (remote+http://localhost:8080), `--nodo` (tracking-sim), `--vehiculos`
(STA1234,SBB5678), `--eventos` por vehículo (10), `--duplicados` fracción reenviada (0.3),
`--intervalo` segundos entre posiciones (10), `--desordenar`.

Para ver el resultado:

- ruta ordenada por momento de generación: http://localhost:8080/carga-uy/api/posiciones?matricula=STA1234
- qué hizo con cada mensaje: `docker compose logs carga-uy | grep ingesta`
- estado de la cola: `curl -s http://localhost:9990/metrics | grep queue_tracking`

Para arrancar de cero se borra la base local: `docker compose down -v`.
