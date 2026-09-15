FROM maven:3.9.11-eclipse-temurin-24 AS build
WORKDIR /app
COPY . .
RUN mvn -B clean install

FROM quay.io/wildfly/wildfly:41.0.0.Final-jdk25

COPY --chown=jboss:jboss config/modules/ /opt/jboss/wildfly/modules/
RUN curl -fsSL -o /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/postgresql-42.7.7.jar https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.7/postgresql-42.7.7.jar

ARG CLI=/opt/jboss/wildfly/bin/jboss-cli.sh

RUN $CLI --commands='embed-server --server-config=standalone-full.xml,\
/subsystem=datasources/jdbc-driver=postgresql:add(driver-name=postgresql,driver-module-name=org.postgresql,driver-class-name=org.postgresql.Driver,driver-xa-datasource-class-name=org.postgresql.xa.PGXADataSource),\
stop-embedded-server'

RUN $CLI --commands='embed-server --server-config=standalone-full.xml,\
data-source add --name=CargaUyDS \
--jndi-name=java:jboss/datasources/CargaUyDS \
--driver-name=postgresql \
--connection-url=jdbc:postgresql://${env.DB_HOST:postgres}:${env.DB_PORT:5432}/${env.DB_NAME:cargauy} \
--user-name=${env.DB_USER:cargauy} \
--password=${env.DB_PASSWORD:cargauy} \
--enabled=true \
--use-java-context=true,\
stop-embedded-server'

RUN $CLI --commands='embed-server --server-config=standalone-full.xml,\
jms-queue add --queue-address=queue_alta_empresa --entries=[java:/jms/queue/queue_alta_empresa java:jboss/exported/jms/queue/queue_alta_empresa],\
stop-embedded-server'

RUN rm -rf /opt/jboss/wildfly/standalone/configuration/standalone_xml_history

RUN /opt/jboss/wildfly/bin/add-user.sh -a -u admin -p admin123 -g guest
RUN /opt/jboss/wildfly/bin/add-user.sh -u admin -p admin123
COPY --from=build /app/carga-uy-ear/target/carga-uy.ear /opt/jboss/wildfly/standalone/deployments/
USER 0
RUN chgrp -R 0 /opt/jboss/wildfly/standalone && chmod -R g=u /opt/jboss/wildfly/standalone
USER 1000

EXPOSE 8080 9990
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-c", "standalone-full.xml", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
