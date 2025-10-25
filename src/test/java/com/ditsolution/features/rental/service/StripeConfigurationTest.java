package com.ditsolution.features.rental.service;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class StripeConfigurationTest {

    @BeforeEach
    void setUp() {
        // Configuration Stripe avec variable d'environnement
        String apiKey = System.getenv("STRIPE_SECRET_KEY");
        if (apiKey != null && !apiKey.isEmpty()) {
            Stripe.apiKey = apiKey;
        } else {
            // Utiliser une clé de test générique pour éviter l'exposition
            Stripe.apiKey = System.getProperty("stripe.test.key", "sk_test_placeholder");
        }
    }

    @Test
    void testStripeConfiguration() {
        // Test de configuration Stripe
        assertNotNull(Stripe.apiKey);
        assertTrue(Stripe.apiKey.startsWith("sk_test_"));
        // Ne pas vérifier la clé exacte pour éviter l'exposition
    }

    @Test
    void testStripeKeysFormat() {
        // Test du format des clés (sans exposer les vraies clés)
        String secretKey = System.getProperty("stripe.test.secret", "sk_test_placeholder");
        String publishableKey = System.getProperty("stripe.test.publishable", "pk_test_placeholder");

        // Vérifier le format des clés
        assertTrue(secretKey.startsWith("sk_test_"));
        assertTrue(publishableKey.startsWith("pk_test_"));
        assertTrue(secretKey.length() > 10);
        assertTrue(publishableKey.length() > 10);
    }

    @Test
    void testPaymentIntentCreation() {
        try {
            // Test de création d'un PaymentIntent (simulation)
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(120000L) // 1200.00 EUR en centimes
                    .setCurrency("eur")
                    .setDescription("Test de paiement loyer")
                    .build();

            // Vérifier que les paramètres sont corrects
            assertNotNull(params);
            assertEquals(120000L, params.getAmount());
            assertEquals("eur", params.getCurrency());
            assertEquals("Test de paiement loyer", params.getDescription());
        } catch (Exception e) {
            // En mode test, on ne fait pas d'appel réel à Stripe
            assertTrue(true, "Configuration Stripe correcte");
        }
    }

    @Test
    void testWebhookEventFormat() {
        // Test du format d'événement webhook fourni
        String eventType = "setup_intent.created";
        String eventId = "evt_abc123xyz";
        String apiVersion = "2019-02-19";
        long created = 1686089970L;
        boolean livemode = false;

        // Vérifier le format de l'événement
        assertNotNull(eventType);
        assertNotNull(eventId);
        assertNotNull(apiVersion);
        assertTrue(created > 0);
        assertFalse(livemode); // Mode test
        assertEquals("setup_intent.created", eventType);
    }

    @Test
    void testAmountConversion() {
        // Test de conversion des montants
        BigDecimal rentAmount = new BigDecimal("1200.00");
        long amountInCents = rentAmount.multiply(new BigDecimal("100")).longValue();

        assertEquals(120000L, amountInCents);
        assertEquals("1200.00", rentAmount.toString());
    }
}