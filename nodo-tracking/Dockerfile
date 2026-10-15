# etapa 1: compilar, con la misma imagen de Maven que usa el central
FROM maven:3.9.11-eclipse-temurin-24 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B package -DskipTests

# etapa 2: solo la JRE y los jars, sin Maven ni el JDK
FROM eclipse-temurin:24-jre
WORKDIR /app
COPY --from=build /app/target/nodo-tracking.jar .
COPY --from=build /app/target/lib lib
# certificado del broker de desarrollo, para la conexion con TLS
COPY tls tls
EXPOSE 8081
CMD ["java", "-cp", "nodo-tracking.jar:lib/*", "tse.cargauy.nodotracking.NodoTracking"]
