package com.ditsolution.features.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class FirebaseConfig {

    @ConfigProperty(name = "firebase.service-account-key", defaultValue = "")
    String serviceAccountKey;

    @ConfigProperty(name = "firebase.project-id", defaultValue = "")
    String projectId;

    private FirebaseApp firebaseApp;

    @PostConstruct
    public void initializeFirebase() {
        try {
            if (serviceAccountKey.isEmpty() || projectId.isEmpty()) {
                Log.warn("Configuration Firebase manquante. Les notifications push ne fonctionneront pas.");
                return;
            }

            GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(serviceAccountKey.getBytes(StandardCharsets.UTF_8))
            );

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build();

            if (FirebaseApp.getApps().isEmpty()) {
                firebaseApp = FirebaseApp.initializeApp(options);
                Log.info("Firebase initialisé avec succès pour le projet: " + projectId);
            } else {
                firebaseApp = FirebaseApp.getInstance();
                Log.info("Firebase déjà initialisé");
            }
        } catch (IOException e) {
            Log.error("Erreur lors de l'initialisation de Firebase: " + e.getMessage(), e);
        }
    }

    @Produces
    @ApplicationScoped
    public FirebaseMessaging firebaseMessaging() {
        if (firebaseApp != null) {
            return FirebaseMessaging.getInstance(firebaseApp);
        }
        return null;
    }

    public boolean isFirebaseConfigured() {
        return firebaseApp != null;
    }
}

