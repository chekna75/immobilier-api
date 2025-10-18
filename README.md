# API Immobilière - Quarkus

Application REST API pour la gestion de biens immobiliers développée avec Quarkus.

## 🚀 Technologies utilisées

- **Quarkus 3.25.4** - Framework Java natif pour le cloud
- **Hibernate ORM avec Panache** - ORM simplifié
- **PostgreSQL** - Base de données
- **Flyway** - Migration de base de données
- **RESTEasy** - API REST
- **Jackson** - Sérialisation JSON
- **SmallRye OpenAPI** - Documentation API
- **Swagger UI** - Interface de test API
- **SmallRye Health** - Health checks

## 📋 Prérequis

- Java 21+
- Maven 3.8+
- PostgreSQL 12+

## 🛠️ Installation et configuration

### 1. Configuration de la base de données

Créez une base de données PostgreSQL :

```sql
CREATE DATABASE immobilier_db;
CREATE USER postgres WITH PASSWORD 'postgres';
GRANT ALL PRIVILEGES ON DATABASE immobilier_db TO postgres;
```

### 2. Configuration de l'application

Modifiez le fichier `src/main/resources/application.properties` selon votre configuration :

```properties
# Base de données
quarkus.datasource.username=votre_utilisateur
quarkus.datasource.password=votre_mot_de_passe
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/immobilier_db
```

## 🏃‍♂️ Lancement de l'application

### Mode développement
```bash
./mvnw quarkus:dev
```

### Mode production
```bash
./mvnw clean package
java -jar target/quarkus-app/quarkus-run.jar
```

## 📚 API Endpoints

### Biens immobiliers

- `GET /api/biens` - Récupérer tous les biens
- `GET /api/biens/{id}` - Récupérer un bien par ID
- `POST /api/biens` - Créer un nouveau bien
- `PUT /api/biens/{id}` - Mettre à jour un bien
- `DELETE /api/biens/{id}` - Supprimer un bien
- `GET /api/biens/search?ville={ville}&type={type}` - Rechercher des biens
- `GET /api/biens/stats` - Statistiques des biens

### Documentation et outils

- `GET /swagger-ui` - Interface Swagger UI
- `GET /openapi` - Spécification OpenAPI
- `GET /health` - Health checks
- `GET /hello` - Endpoint de test

## 🏗️ Structure du projet

```
src/main/java/com/ditsolution/immobilier/
├── entity/
│   └── Bien.java              # Entité Bien immobilier
├── resource/
│   └── BienResource.java      # Contrôleur REST
├── health/
│   └── DatabaseHealthCheck.java # Health check personnalisé
└── HelloResource.java         # Endpoint de test
```

## 📊 Modèle de données

### Entité Bien

```java
public class Bien extends PanacheEntity {
    public String titre;
    public String description;
    public BigDecimal prix;
    public String adresse;
    public String codePostal;
    public String ville;
    public Integer surfaceM2;
    public Integer nombrePieces;
    public TypeBien type;
    public StatutBien statut;
    public LocalDateTime dateCreation;
    public LocalDateTime dateModification;
}
```

### Types de biens
- `APPARTEMENT`
- `MAISON`
- `TERRAIN`
- `BUREAUX`
- `COMMERCE`

### Statuts
- `DISPONIBLE`
- `VENDU`
- `EN_NEGOCIATION`
- `RETIRE`

## 🔧 Configuration

### Variables d'environnement

Vous pouvez surcharger la configuration avec des variables d'environnement :

```bash
export QUARKUS_DATASOURCE_USERNAME=mon_user
export QUARKUS_DATASOURCE_PASSWORD=mon_password
export QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:5432/immobilier_db
```

### Profils

- **dev** : Mode développement avec base de données H2 en mémoire
- **prod** : Mode production avec PostgreSQL

## 🧪 Tests

```bash
# Tests unitaires
./mvnw test

# Tests d'intégration
./mvnw verify
```

## 📦 Build et déploiement

### Build JAR
```bash
./mvnw clean package
```

### Build Docker
```bash
./mvnw clean package -Dquarkus.package.type=docker
```

### Build Native (GraalVM)
```bash
./mvnw clean package -Pnative
```

## 🔍 Monitoring et observabilité

- **Health checks** : `GET /health`
- **Metrics** : `GET /q/metrics` (si extension metrics ajoutée)
- **Logs** : Configuration dans `application.properties`

## 🚀 Déploiement

### Docker
```bash
docker build -f src/main/docker/Dockerfile.jvm -t immobilier-api .
docker run -p 8080:8080 immobilier-api
```

### Kubernetes
```bash
kubectl apply -f k8s/
```

## 📝 Exemples d'utilisation

### Créer un bien

```bash
curl -X POST http://localhost:8080/api/biens \
  -H "Content-Type: application/json" \
  -d '{
    "titre": "Appartement T3",
    "description": "Bel appartement dans le centre-ville",
    "prix": 250000.00,
    "adresse": "123 Rue de la Paix",
    "codePostal": "75001",
    "ville": "Paris",
    "surfaceM2": 75,
    "nombrePieces": 3,
    "type": "APPARTEMENT"
  }'
```

### Récupérer tous les biens

```bash
curl http://localhost:8080/api/biens
```

### Rechercher par ville

```bash
curl "http://localhost:8080/api/biens/search?ville=Paris"
```

## 🤝 Contribution

1. Fork le projet
2. Créez une branche feature (`git checkout -b feature/AmazingFeature`)
3. Commit vos changements (`git commit -m 'Add some AmazingFeature'`)
4. Push vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrez une Pull Request

## 📄 Licence

Ce projet est sous licence MIT. Voir le fichier `LICENSE` pour plus de détails.

## 📞 Support

Pour toute question ou problème, veuillez ouvrir une issue sur GitHub.
