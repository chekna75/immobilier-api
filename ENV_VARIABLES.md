# Variables d'environnement

Ce fichier documente toutes les variables d'environnement nécessaires pour faire fonctionner l'application.

## Base de données PostgreSQL

```bash
export DB_USER=postgres
export DB_PASS=postgres
export DB_URL=jdbc:postgresql://localhost:5432/immobilier_db
```

## Configuration Email (SMTP)

```bash
export SMTP_HOST=smtp.gmail.com
export SMTP_PORT=587
export SMTP_USER=your-email@gmail.com
export SMTP_PASS=your-app-password
export SMTP_SSL=false
export SMTP_STARTTLS=true
```

## Configuration SMS (Hub2)

```bash
export SMS_API_KEY=your-hub2-api-key
```

## Configuration JWT

Les fichiers de clés JWT doivent être placés dans `src/main/resources/` :

- `publicKey.pem` - Clé publique pour vérifier les tokens
- `privateKey.pem` - Clé privée pour signer les tokens

### Génération des clés JWT

```bash
# Générer la clé privée
openssl genrsa -out privateKey.pem 2048

# Extraire la clé publique
openssl rsa -pubout -in privateKey.pem -out publicKey.pem
```

## Exemple de fichier .env

Créez un fichier `.env` à la racine du projet :

```bash
# Base de données
DB_USER=postgres
DB_PASS=postgres
DB_URL=jdbc:postgresql://localhost:5432/immobilier_db

# Email
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your-email@gmail.com
SMTP_PASS=your-app-password
SMTP_SSL=false
SMTP_STARTTLS=true

# SMS
SMS_API_KEY=your-hub2-api-key
```

## Lancement avec variables d'environnement

```bash
# Charger les variables depuis un fichier .env
source .env

# Lancer l'application
./mvnw quarkus:dev
```

## Configuration pour différents environnements

### Développement
```bash
export DB_URL=jdbc:postgresql://localhost:5432/immobilier_dev
export SMTP_HOST=localhost
export SMTP_PORT=1025  # MailHog ou similaire
```

### Production
```bash
export DB_URL=jdbc:postgresql://prod-server:5432/immobilier_prod
export SMTP_HOST=smtp.provider.com
export SMTP_PORT=587
export SMTP_SSL=true
```
