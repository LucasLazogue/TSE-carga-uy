# broker de mensajeria (DA02): Apache Artemis con la configuracion de carga.uy
FROM apache/artemis:2.57.0-alpine

COPY --chown=artemis:artemis etc/ /opt/carga-uy/etc/
COPY --chown=artemis:artemis --chmod=755 arranque.sh /opt/carga-uy/arranque.sh

# 61616: instancias de WildFly; 61617: nodos perifericos, con TLS; 8161: consola
EXPOSE 61616 61617 8161
ENTRYPOINT ["/opt/carga-uy/arranque.sh"]
CMD ["run"]
