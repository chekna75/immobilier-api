# 🔥 Configuration Firebase pour l'API Java

## 📋 Prérequis

1. **Compte Firebase** : Créez un projet sur [Firebase Console](https://console.firebase.google.com/)
2. **Service Account** : Générez une clé de service pour l'API
3. **Configuration** : Suivez les étapes ci-dessous

## 🚀 Configuration

### **1. Créer un projet Firebase**

1. Allez sur [Firebase Console](https://console.firebase.google.com/)
2. Cliquez sur "Ajouter un projet"
3. Nommez votre projet (ex: `app-immo-notifications`)
4. Activez Google Analytics (optionnel)

### **2. Activer Firebase Cloud Messaging**

1. Dans votre projet Firebase, allez dans **Messaging**
2. Cliquez sur "Commencer"
3. Configurez les paramètres de notification

### **3. Générer une clé de service**

1. Allez dans **Paramètres du projet** → **Comptes de service**
2. Cliquez sur "Générer une nouvelle clé privée"
3. Téléchargez le fichier JSON
4. Renommez-le : `firebase-adminsdk.json`
5. **Placez-le dans** : `src/main/resources/firebase-adminsdk.json`

### **4. Configuration Maven**

Ajoutez la dépendance Firebase dans votre `pom.xml` :

```xml
<dependency>
    <groupId>com.google.firebase</groupId>
    <artifactId>firebase-admin</artifactId>
    <version>9.2.0</version>
</dependency>
```

### **5. Configuration Java**

Créez une classe de configuration Firebase :

```java
@ApplicationScoped
public class FirebaseInitializer {
    
    private static final Logger logger = LoggerFactory.getLogger(FirebaseInitializer.class);
    
    @PostConstruct
    public void initializeFirebase() {
        try {
            // Charger le fichier de configuration depuis les ressources
            InputStream serviceAccount = getClass()
                .getClassLoader()
                .getResourceAsStream("firebase-adminsdk.json");
                
            if (serviceAccount == null) {
                logger.error("Fichier firebase-adminsdk.json non trouvé dans src/main/resources/");
                return;
            }
            
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();
                
            FirebaseApp.initializeApp(options);
            logger.info("Firebase initialisé avec succès");
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'initialisation Firebase: " + e.getMessage(), e);
        }
    }
}
```

### **6. Service de Notifications**

Créez un service pour envoyer les notifications :

```java
@ApplicationScoped
public class FirebaseNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(FirebaseNotificationService.class);
    
    public void sendNotification(String token, String title, String body, Map<String, String> data) {
        try {
            Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .putAllData(data)
                .build();
                
            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("Notification envoyée avec succès: " + response);
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de la notification: " + e.getMessage(), e);
        }
    }
    
    public void sendToTopic(String topic, String title, String body, Map<String, String> data) {
        try {
            Message message = Message.builder()
                .setTopic(topic)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .putAllData(data)
                .build();
                
            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("Notification envoyée au topic " + topic + ": " + response);
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de la notification au topic: " + e.getMessage(), e);
        }
    }
}
```

### **7. Endpoints de Notification**

Créez des endpoints pour déclencher les notifications :

```java
@Path("/api/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationResource {
    
    @Inject
    FirebaseNotificationService notificationService;
    
    @POST
    @Path("/send")
    public Response sendNotification(NotificationRequest request) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("type", request.getType());
            data.put("navigation", request.getNavigation());
            data.put("userId", request.getUserId());
            
            notificationService.sendNotification(
                request.getToken(),
                request.getTitle(),
                request.getBody(),
                data
            );
            
            return Response.ok().entity(Map.of("success", true)).build();
            
        } catch (Exception e) {
            return Response.status(500)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
    
    @POST
    @Path("/send-to-topic")
    public Response sendToTopic(TopicNotificationRequest request) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("type", request.getType());
            data.put("navigation", request.getNavigation());
            
            notificationService.sendToTopic(
                request.getTopic(),
                request.getTitle(),
                request.getBody(),
                data
            );
            
            return Response.ok().entity(Map.of("success", true)).build();
            
        } catch (Exception e) {
            return Response.status(500)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
}
```

## 🚨 Sécurité

### **⚠️ Important : Ne jamais commiter les secrets !**

- ✅ Utilisez `.gitignore` pour exclure les fichiers de clés
- ✅ Placez le fichier dans `src/main/resources/` (non versionné)
- ✅ Utilisez des variables d'environnement en production
- ✅ Régénérez les clés si elles sont exposées

### **Fichiers à exclure du Git**
```
# Dans .gitignore
**/firebase-adminsdk-*.json
**/GoogleService-Info.plist
**/google-services.json
```

## 🧪 Tests

### **Test des notifications**
```bash
# Test avec curl
curl -X POST http://localhost:8080/api/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "token": "your-fcm-token",
    "title": "Test Notification",
    "body": "Ceci est un test",
    "type": "test",
    "navigation": "home"
  }'
```

### **Test avec topic**
```bash
curl -X POST http://localhost:8080/api/notifications/send-to-topic \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "new_listings",
    "title": "Nouvelle annonce",
    "body": "Une nouvelle annonce correspond à vos critères",
    "type": "new_listing",
    "navigation": "search_results"
  }'
```

## 📊 Types de Notifications Supportés

### **1. 🏠 Nouvelles annonces**
- **Topic** : `new_listings`
- **Navigation** : `search_results`
- **Déclencheur** : Nouvelle annonce correspondant aux critères

### **2. 💬 Messages reçus**
- **Topic** : `messages`
- **Navigation** : `chat`
- **Déclencheur** : Nouveau message dans une conversation

### **3. 📝 Changements de statut**
- **Topic** : `status_changes`
- **Navigation** : `my_listings`
- **Déclencheur** : Modification du statut d'une annonce

### **4. 💳 Rappels de paiement**
- **Topic** : `payment_reminders`
- **Navigation** : `payment`
- **Déclencheur** : Paiement dû ou abonnement à renouveler

## 🚀 Démarrage

### **Développement**
```bash
./mvnw compile quarkus:dev
```

### **Production**
```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

## 📞 Support

Si vous rencontrez des problèmes :
1. **Vérifiez la configuration** Firebase
2. **Vérifiez les logs** de l'application
3. **Testez avec les endpoints** fournis
4. **Consultez la documentation** Firebase

---

**🎉 Votre API Java avec Firebase est prête !**
