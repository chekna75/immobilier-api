# Dockerfile pour l'API Quarkus
# Build en deux étapes pour optimiser la taille de l'image

# Étape 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copier les fichiers de configuration Maven
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw

# Télécharger les dépendances (cache layer)
RUN ./mvnw dependency:go-offline -B

# Copier le code source
COPY src ./src

# Build de l'application (sans tests)
RUN ./mvnw clean package -DskipTests

# Étape 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Créer un utilisateur non-root pour la sécurité
RUN addgroup -S quarkus && adduser -S quarkus -G quarkus

# Copier le JAR de l'application
COPY --from=build --chown=quarkus:quarkus /app/target/quarkus-app/lib /app/lib
COPY --from=build --chown=quarkus:quarkus /app/target/quarkus-app/*.jar /app
COPY --from=build --chown=quarkus:quarkus /app/target/quarkus-app/app /app/app
COPY --from=build --chown=quarkus:quarkus /app/target/quarkus-app/quarkus /app/quarkus

# Passer à l'utilisateur non-root
USER quarkus

# Exposer le port
EXPOSE 8080

# Commande de démarrage
ENTRYPOINT ["java", "-jar", "/app/quarkus-run.jar"]

