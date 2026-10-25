FROM maven:3.9.11-eclipse-temurin-24 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -B package

FROM quay.io/wildfly/wildfly:41.0.0.Final-jdk25

COPY --chown=jboss:jboss config/modules/ /opt/jboss/wildfly/modules/
RUN curl -fsSL -o /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/postgresql-42.7.7.jar https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.7/postgresql-42.7.7.jar

ARG CLI=/opt/jboss/wildfly/bin/jboss-cli.sh

RUN $CLI --commands='embed-server,\
/subsystem=datasources/jdbc-driver=postgresql:add(driver-name=postgresql,driver-module-name=org.postgresql,driver-class-name=org.postgresql.Driver,driver-xa-datasource-class-name=org.postgresql.xa.PGXADataSource),\
stop-embedded-server'

RUN $CLI --commands='embed-server,\
data-source add --name=NodoBalanzasDS \
--jndi-name=java:jboss/datasources/NodoBalanzasDS \
--driver-name=postgresql \
--connection-url=jdbc:postgresql://${env.DB_HOST:localhost}:${env.DB_PORT:5432}/${env.DB_NAME:balanzas} \
--user-name=${env.DB_USER:balanzas} \
--password=${env.DB_PASSWORD:balanzas} \
--enabled=true \
--use-java-context=true,\
stop-embedded-server'

RUN $CLI --commands='embed-server,\
/subsystem=undertow/application-security-domain=other:write-attribute(name=integrated-jaspi,value=false),\
stop-embedded-server'

RUN rm -rf /opt/jboss/wildfly/standalone/configuration/standalone_xml_history

COPY --from=build /app/target/nodo-balanzas.war /opt/jboss/wildfly/standalone/deployments/
USER 0
RUN chgrp -R 0 /opt/jboss/wildfly/standalone && chmod -R g=u /opt/jboss/wildfly/standalone
USER 1000

EXPOSE 8080 9990
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
