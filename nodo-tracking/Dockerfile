# etapa 1: compilar, con la misma imagen de Maven que usa el central
FROM maven:3.9.11-eclipse-temurin-24 AS build
WORKDIR /app
# primero solo el pom: si cambia el codigo pero no las dependencias, Docker reusa esta capa y no las vuelve a bajar
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B package -DskipTests

# etapa 2: solo la JRE y los jars, sin Maven ni el JDK
FROM eclipse-temurin:24-jre
WORKDIR /app
COPY --from=build /app/target/nodo-tracking.jar .
COPY --from=build /app/target/lib lib
EXPOSE 8081
CMD ["java", "-cp", "nodo-tracking.jar:lib/*", "tse.cargauy.nodotracking.NodoTracking"]
