# Dockerfile pour l'API Quarkus
# Build en deux étapes pour optimiser la taille de l'image
# Optimisé pour Railway et autres plateformes cloud

# Étape 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copier les fichiers de configuration Maven
COPY pom.xml .

# Télécharger les dépendances (cache layer pour optimisation)
# Utilise mvn directement (déjà présent dans l'image maven)
RUN mvn dependency:go-offline -B

# Copier le code source
COPY src ./src

# Build de l'application avec le profil production
RUN mvn clean package -DskipTests -Dquarkus.profile=prod

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

# Exposer le port (Railway détecte automatiquement le port 8080)
EXPOSE 8080

# Variables d'environnement par défaut
ENV QUARKUS_HTTP_HOST=0.0.0.0
ENV QUARKUS_HTTP_PORT=8080

# Commande de démarrage
ENTRYPOINT ["java", "-jar", "/app/quarkus-run.jar"]

