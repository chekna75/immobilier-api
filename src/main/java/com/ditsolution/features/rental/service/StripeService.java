package com.ditsolution.features.rental.service;

import com.ditsolution.features.rental.dto.InitiatePaymentRequest;
import com.ditsolution.features.rental.dto.PaymentResponse;
import com.ditsolution.features.rental.entity.PaymentTransactionEntity;
import com.ditsolution.features.rental.entity.RentPaymentEntity;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Customer;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.CustomerCreateParams;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class StripeService {

    private static final Logger LOG = Logger.getLogger(StripeService.class);

    @ConfigProperty(name = "stripe.secret.key")
    String secretKey;

    @ConfigProperty(name = "stripe.publishable.key")
    String publishableKey;

    @ConfigProperty(name = "app.base.url", defaultValue = "http://localhost:8080")
    String baseUrl;

    public PaymentResponse initiatePayment(InitiatePaymentRequest request, RentPaymentEntity rentPayment) {
        try {
            // Initialiser Stripe
            Stripe.apiKey = secretKey;

            // Créer ou récupérer le client Stripe
            String customerId = getOrCreateCustomer(rentPayment.getContract().getTenant());

            // Créer le PaymentIntent
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(rentPayment.getAmount().multiply(new BigDecimal("100")).longValue()) // Convertir en centimes
                    .setCurrency("eur")
                    .setCustomer(customerId)
                    .setDescription("Paiement loyer - " + rentPayment.getContract().getProperty().getTitle())
                    .putAllMetadata(createMetadata(rentPayment))
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.AUTOMATIC)
                    .setReturnUrl(baseUrl + "/api/rental/payments/return")
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // Créer l'entité de transaction
            PaymentTransactionEntity transaction = new PaymentTransactionEntity();
            transaction.setRentPayment(rentPayment);
            transaction.setStripePaymentIntentId(paymentIntent.getId());
            transaction.setAmount(rentPayment.getAmount());
            transaction.setCurrency("EUR");
            transaction.setStatus(PaymentTransactionEntity.TransactionStatus.PENDING);
            transaction.setPaymentMethod(PaymentTransactionEntity.PaymentMethod.CARD);
            transaction.setStripeCustomerId(customerId);
            transaction.setClientIp(request.getClientIp());
            transaction.setUserAgent(request.getUserAgent());
            transaction.setSuccessUrl(baseUrl + "/api/rental/payments/success");
            transaction.setCancelUrl(baseUrl + "/api/rental/payments/cancel");
            transaction.setDescription("Paiement loyer - " + rentPayment.getContract().getProperty().getTitle());
            transaction.setMetadata(createMetadataJson(rentPayment));
            transaction.persist();

            // Créer la réponse
            PaymentResponse response = new PaymentResponse();
            response.setPaymentUrl(paymentIntent.getClientSecret());
            response.setTransactionId(paymentIntent.getId());
            response.setStatus("PENDING");
            response.setMessage("Paiement initié avec succès");

            return response;

        } catch (StripeException e) {
            LOG.error("Erreur Stripe lors de l'initiation du paiement", e);
            throw new RuntimeException("Erreur lors de l'initiation du paiement: " + e.getMessage());
        } catch (Exception e) {
            LOG.error("Erreur lors de l'initiation du paiement", e);
            throw new RuntimeException("Erreur lors de l'initiation du paiement", e);
        }
    }

    public boolean verifyPayment(String paymentIntentId) {
        try {
            Stripe.apiKey = secretKey;
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            return "succeeded".equals(paymentIntent.getStatus());
        } catch (StripeException e) {
            LOG.error("Erreur lors de la vérification du paiement", e);
            return false;
        }
    }

    public PaymentIntent getPaymentIntent(String paymentIntentId) {
        try {
            Stripe.apiKey = secretKey;
            return PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException e) {
            LOG.error("Erreur lors de la récupération du PaymentIntent", e);
            throw new RuntimeException("Erreur lors de la récupération du paiement", e);
        }
    }

    private String getOrCreateCustomer(com.ditsolution.features.auth.entity.UserEntity user) throws StripeException {
        // Vérifier si le client existe déjà (par email)
        Map<String, Object> params = new HashMap<>();
        params.put("email", user.email);
        
        // Pour simplifier, on crée toujours un nouveau client
        // Dans un vrai projet, on devrait vérifier l'existence d'abord
        CustomerCreateParams customerParams = CustomerCreateParams.builder()
                .setEmail(user.email)
                .setName(user.firstName + " " + user.lastName)
                .setPhone(user.phoneE164)
                .build();

        Customer customer = Customer.create(customerParams);
        return customer.getId();
    }

    private Map<String, String> createMetadata(RentPaymentEntity rentPayment) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("rent_payment_id", rentPayment.id.toString());
        metadata.put("contract_id", rentPayment.getContract().id.toString());
        metadata.put("property_id", rentPayment.getContract().getProperty().getId().toString());
        metadata.put("tenant_id", rentPayment.getContract().getTenant().id.toString());
        metadata.put("owner_id", rentPayment.getContract().getOwner().id.toString());
        return metadata;
    }

    private String createMetadataJson(RentPaymentEntity rentPayment) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("rent_payment_id", rentPayment.id);
        metadata.put("contract_id", rentPayment.getContract().id);
        metadata.put("property_id", rentPayment.getContract().getProperty().getId());
        metadata.put("tenant_id", rentPayment.getContract().getTenant().id);
        metadata.put("owner_id", rentPayment.getContract().getOwner().id);
        
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(metadata);
        } catch (Exception e) {
            return "{}";
        }
    }
}
