# Variables d'environnement

Ce fichier documente toutes les variables d'environnement nécessaires pour faire fonctionner l'application.

## 🚨 IMPORTANT - Sécurité

**Ne JAMAIS commit de secrets dans le code !** Tous les secrets doivent être gérés via des variables d'environnement.

Les fichiers suivants sont ignorés par git :
- `.env.dev` - Variables d'environnement de développement
- `.env.prod` - Variables d'environnement de production
- `application-dev.properties` - Configuration de développement
- `application-prod.properties` - Configuration de production

## 📋 Configuration par environnement

### Développement

1. **Copiez le fichier d'exemple :**
   ```bash
   cp .env.dev.example .env.dev
   ```

2. **Éditez `.env.dev` avec vos vraies valeurs de développement**

3. **Chargez les variables d'environnement :**
   ```bash
   source .env.dev
   ```

4. **Lancez l'application en mode dev :**
   ```bash
   ./mvnw quarkus:dev
   ```

### Production

1. **Copiez le fichier d'exemple :**
   ```bash
   cp .env.prod.example .env.prod
   ```

2. **Éditez `.env.prod` avec vos vraies valeurs PRODUCTION**

3. **Configurez les variables d'environnement sur votre serveur/hébergeur**

4. **Lancez l'application en mode production :**
   ```bash
   ./mvnw clean package -Dquarkus.profile=prod
   java -jar target/quarkus-app/quarkus-run.jar
   ```

## 📝 Variables d'environnement requises

### Base de données PostgreSQL

```bash
DB_USERNAME=postgres          # Utilisateur de la base de données
DB_PASSWORD=postgres          # Mot de passe de la base de données
DB_URL=jdbc:postgresql://localhost:5432/immobilier_db  # URL de connexion
```

### Configuration Email (SMTP)

```bash
MAIL_FROM=your-email@gmail.com          # Email expéditeur
MAIL_HOST=smtp.gmail.com                 # Serveur SMTP
MAIL_PORT=587                            # Port SMTP
MAIL_USERNAME=your-email@gmail.com       # Utilisateur SMTP
MAIL_PASSWORD=your-app-password          # Mot de passe SMTP (app password pour Gmail)
MAIL_START_TLS=true                      # Activer STARTTLS
```

### Configuration SMS (Hub2)

```bash
SMS_API_KEY=your-hub2-api-key
```

### Configuration JWT

Les fichiers de clés JWT doivent être placés dans `src/main/resources/` :

- `publicKey.pem` - Clé publique pour vérifier les tokens
- `privateKey.pem` - Clé privée pour signer les tokens

**Variables d'environnement JWT :**
```bash
JWT_PUBLIC_KEY_LOCATION=classpath:publicKey.pem
JWT_PRIVATE_KEY_LOCATION=classpath:privateKey.pem
JWT_ISSUER=immobilier-dev  # Pour dev, ou https://your-domain.com pour prod
```

**Génération des clés JWT :**
```bash
# Générer la clé privée
openssl genrsa -out privateKey.pem 2048

# Extraire la clé publique
openssl rsa -pubout -in privateKey.pem -out publicKey.pem
```

### Configuration Stripe

```bash
STRIPE_SECRET_KEY=sk_test_...      # Clé secrète (test ou live)
STRIPE_PUBLISHABLE_KEY=pk_test_... # Clé publique (test ou live)
STRIPE_WEBHOOK_SECRET=whsec_...    # Secret du webhook
```

### Configuration AWS S3

```bash
AWS_S3_REGION=eu-north-1
AWS_S3_BUCKET=app-immo
AWS_S3_ACCESS_KEY=your-access-key
AWS_S3_SECRET_KEY=your-secret-key
```

### Configuration Firebase

```bash
FIREBASE_PROJECT_ID=app-immo-notifications
FIREBASE_SERVICE_ACCOUNT_KEY=your-service-account-key
FIREBASE_CREDENTIALS_PATH=classpath:firebase-credentials.json
```

### Configuration Application

```bash
APP_BASE_URL=http://localhost:8080  # Pour dev
# ou
APP_BASE_URL=https://api.your-domain.com  # Pour prod
```

### Configuration CORS (Production uniquement)

```bash
CORS_ORIGINS=https://your-domain.com,https://www.your-domain.com
```

## 🔧 Utilisation avec Maven

### Variables d'environnement Flyway

Pour les migrations Flyway via Maven, vous pouvez utiliser :

```bash
export FLYWAY_URL=jdbc:postgresql://localhost:5432/immobilier
export FLYWAY_USER=postgres
export FLYWAY_PASSWORD=postgres
```

Ou utilisez les variables DB_* :
```bash
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
```

## 📚 Profils Quarkus

L'application utilise des profils Quarkus pour différents environnements :

- **Développement** : `application-dev.properties` (activé automatiquement en dev)
- **Production** : `application-prod.properties` (activé avec `-Dquarkus.profile=prod`)

Pour utiliser le profil production :
```bash
./mvnw clean package -Dquarkus.profile=prod
```

## ✅ Checklist avant déploiement

- [ ] Tous les secrets sont dans des variables d'env (pas de hardcoding)
- [ ] Le fichier `.env.prod` est configuré avec les vraies valeurs
- [ ] Les clés Stripe LIVE sont configurées (pas de clés de test)
- [ ] CORS est configuré avec les vrais domaines de production
- [ ] Les logs SQL sont désactivés en production
- [ ] Swagger UI est désactivé en production
- [ ] Les clés JWT sont générées et sécurisées
