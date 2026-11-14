FROM maven:3.9.11-eclipse-temurin-24 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
# los tests validan el archivo de personas que va en la imagen
COPY personas personas
RUN mvn -B package

FROM quay.io/wildfly/wildfly:41.0.0.Final-jdk25

ARG CLI=/opt/jboss/wildfly/bin/jboss-cli.sh

COPY --chown=jboss:jboss config/observabilidad.cli /opt/jboss/observabilidad.cli
RUN $CLI --file=/opt/jboss/observabilidad.cli

# el WSDL publicado usa la direccion del pedido, no la IP interna del contenedor
RUN $CLI --commands='embed-server,\
/subsystem=webservices:write-attribute(name=wsdl-host,value=jbossws.undefined.host),\
stop-embedded-server'

RUN rm -rf /opt/jboss/wildfly/standalone/configuration/standalone_xml_history

COPY --from=build /app/target/pdi-suplantador.war /opt/jboss/wildfly/standalone/deployments/
# el Compose monta encima la carpeta del repo, para editar las personas sin reconstruir
COPY personas /config/personas
ENV PDI_PERSONAS_ARCHIVO=/config/personas/personas.json
USER 0
RUN chgrp -R 0 /opt/jboss/wildfly/standalone && chmod -R g=u /opt/jboss/wildfly/standalone
USER 1000

EXPOSE 8080 9990
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
