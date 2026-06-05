# Ohne syntax=docker/dockerfile:1: vermeidet Docker-Hub-Fetch (Netzwerk-Timeout)
# Sans --mount=type=cache : build plus lent mais fonctionne sans accès registry

FROM maven:3.9.8-eclipse-temurin-21 AS build

WORKDIR /app

# pom.xml kopieren und Abhängigkeiten herunterladen
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Quellcode kopieren und kompilieren
COPY src ./src
RUN mvn clean package -B -DskipTests -q

# Schlankes Image: JRE statt JDK (reduziert Größe ~200 MB)
FROM eclipse-temurin:21-jre
WORKDIR /app

# tini: minimaler Init, um SIGTERM korrekt an Java-Prozess weiterzuleiten (vermeidet Blockade beim Stop)
RUN apt-get update && apt-get install -y --no-install-recommends tini \
    && rm -rf /var/lib/apt/lists/*

# Copy the built JAR file from the build stage
COPY --from=build /app/target/java-backend-1.0-SNAPSHOT.jar /app/

# Port for the HTTP-Server
EXPOSE 8080

# tini als PID 1 → Java erhält SIGTERM korrekt beim docker stop
ENTRYPOINT ["/usr/bin/tini", "--", "java", "-jar", "/app/java-backend-1.0-SNAPSHOT.jar"]