FROM maven:3.9.11-eclipse-temurin-24 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -B package

FROM quay.io/wildfly/wildfly:41.0.0.Final-jdk25
COPY --from=build /app/target/nodo-balanzas.war /opt/jboss/wildfly/standalone/deployments/
EXPOSE 8080 9990
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
