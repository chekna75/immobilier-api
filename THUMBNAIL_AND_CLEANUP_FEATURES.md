# Fonctionnalités Avancées de Gestion des Images

Ce document décrit les nouvelles fonctionnalités implémentées pour la gestion avancée des images : génération de miniatures et nettoyage automatique des images S3 non utilisées.

## 🖼️ Génération de Miniatures

### Vue d'ensemble
Le système génère automatiquement des miniatures (thumbnails) pour toutes les images uploadées, permettant d'améliorer les performances d'affichage et de réduire la bande passante.

### Fonctionnalités
- **Génération automatique** : Les miniatures sont générées automatiquement après l'upload d'une image
- **Tâches planifiées** : Génération en lot des miniatures manquantes toutes les heures
- **Configuration flexible** : Dimensions et qualité des miniatures configurables
- **Support ImageMagick** : Utilisation d'ImageMagick pour un traitement d'images de qualité professionnelle

### Configuration
```properties
# Dimensions des miniatures
app.thumbnail.width=300
app.thumbnail.height=200

# Qualité JPEG (1-100)
app.thumbnail.quality=85

# Chemin vers ImageMagick
imagemagick.path=/usr/bin/
```

### API Endpoints

#### Génération manuelle (Admin)
```http
POST /admin/storage/thumbnails/generate
Authorization: Bearer <admin-token>
```

#### Statistiques des miniatures
```http
GET /admin/storage/stats
Authorization: Bearer <admin-token>
```

### Base de données
Nouveaux champs ajoutés à la table `uploaded_images` :
- `thumbnail_s3_key` : Clé S3 de la miniature
- `thumbnail_public_url` : URL publique de la miniature
- `thumbnail_generated` : Indicateur de génération
- `image_width` : Largeur de l'image originale
- `image_height` : Hauteur de l'image originale

## 🧹 Nettoyage Automatique S3

### Vue d'ensemble
Le système nettoie automatiquement les images non utilisées stockées dans S3 pour optimiser les coûts de stockage et maintenir un environnement propre.

### Fonctionnalités
- **Nettoyage automatique** : Suppression des images non utilisées depuis plus de 30 jours
- **Nettoyage complet** : Option pour supprimer toutes les images non utilisées
- **Suppression des miniatures** : Nettoyage automatique des miniatures associées
- **Tâches planifiées** : Nettoyage quotidien et hebdomadaire
- **Logs détaillés** : Traçabilité complète des opérations de nettoyage

### Configuration
```properties
# Nombre de jours avant suppression des images non utilisées
app.cleanup.unused-images-days=30

# Activation des tâches planifiées
app.scheduler.thumbnail-generation.enabled=true
app.scheduler.s3-cleanup.enabled=true
```

### API Endpoints

#### Nettoyage des images non utilisées (Admin)
```http
POST /admin/storage/cleanup/unused
Authorization: Bearer <admin-token>
```

#### Nettoyage complet (Admin)
```http
POST /admin/storage/cleanup/all-unused
Authorization: Bearer <admin-token>
```

#### Statistiques de nettoyage
```http
GET /admin/storage/stats
Authorization: Bearer <admin-token>
```

## ⏰ Tâches Planifiées

### Génération de Miniatures
- **Fréquence** : Toutes les heures
- **Action** : Génère les miniatures manquantes
- **Cron** : `0 0 * * * ?`

### Nettoyage Quotidien
- **Fréquence** : Tous les jours à 2h du matin
- **Action** : Supprime les images non utilisées depuis plus de 30 jours
- **Cron** : `0 0 2 * * ?`

### Nettoyage Complet
- **Fréquence** : Tous les dimanches à 3h du matin
- **Action** : Supprime toutes les images non utilisées
- **Cron** : `0 0 3 * * SUN`

### Maintenance Hebdomadaire
- **Fréquence** : Tous les lundis à 1h du matin
- **Action** : Génération de miniatures + nettoyage
- **Cron** : `0 0 1 * * MON`

## 🛠️ Installation et Configuration

### Prérequis
1. **ImageMagick** installé sur le serveur
2. **Permissions S3** pour lecture/écriture/suppression
3. **Base de données** avec les nouvelles migrations

### Installation d'ImageMagick

#### Ubuntu/Debian
```bash
sudo apt-get update
sudo apt-get install imagemagick
```

#### CentOS/RHEL
```bash
sudo yum install ImageMagick
```

#### macOS
```bash
brew install imagemagick
```

### Migration de Base de Données
```sql
-- Exécuter la migration V12__add_thumbnail_support.sql
-- Ajoute les champs pour les miniatures
```

### Variables d'Environnement
```bash
# Configuration S3 (déjà existante)
AWS_S3_REGION=eu-north-1
AWS_S3_BUCKET=app-immo
AWS_S3_ACCESS_KEY=your-access-key
AWS_S3_SECRET_KEY=your-secret-key

# Configuration des miniatures
APP_THUMBNAIL_WIDTH=300
APP_THUMBNAIL_HEIGHT=200
APP_THUMBNAIL_QUALITY=85
IMAGEMAGICK_PATH=/usr/bin/

# Configuration du nettoyage
APP_CLEANUP_UNUSED_IMAGES_DAYS=30
```

## 📊 Monitoring et Logs

### Logs Importants
- Génération de miniatures : `ThumbnailService`
- Nettoyage S3 : `S3CleanupService`
- Tâches planifiées : `ScheduledTasksService`

### Métriques Disponibles
- Nombre d'images sans miniatures
- Nombre d'images non utilisées
- Nombre d'images anciennes non utilisées
- Statistiques de nettoyage (images supprimées, erreurs)

## 🔧 Dépannage

### Problèmes Courants

#### ImageMagick non trouvé
```
Erreur: ImageMagick non trouvé
Solution: Vérifier le chemin dans imagemagick.path
```

#### Permissions S3 insuffisantes
```
Erreur: Access Denied lors de la suppression
Solution: Vérifier les permissions IAM pour S3
```

#### Miniatures non générées
```
Vérifier:
1. ImageMagick installé et accessible
2. Permissions de lecture S3
3. Logs du ThumbnailService
```

### Commandes de Diagnostic
```bash
# Vérifier ImageMagick
convert --version

# Tester la génération de miniature
convert input.jpg -resize 300x200^ -gravity center -crop 300x200+0+0 -quality 85 output.jpg
```

## 🚀 Utilisation

### Pour les Développeurs
1. Les miniatures sont générées automatiquement
2. Utiliser `thumbnailPublicUrl` pour afficher les miniatures
3. Les tâches de nettoyage sont automatiques

### Pour les Administrateurs
1. Utiliser les endpoints `/admin/storage/*` pour la gestion manuelle
2. Surveiller les logs pour les erreurs
3. Ajuster la configuration selon les besoins

## 📈 Performance

### Optimisations
- Génération asynchrone des miniatures
- Tâches planifiées pour éviter les pics de charge
- Nettoyage par lots pour les grandes quantités
- Logs structurés pour le monitoring

### Recommandations
- Surveiller l'espace disque S3
- Ajuster la fréquence des tâches selon le volume
- Monitorer les performances ImageMagick
- Configurer des alertes sur les erreurs de nettoyage
