package com.ditsolution.features.rental.resource;

import com.ditsolution.features.rental.service.RentalService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/api/rental/stripe")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StripeWebhookResource {

    private static final Logger LOG = Logger.getLogger(StripeWebhookResource.class);

    @Inject
    RentalService rentalService;

    @ConfigProperty(name = "stripe.webhook.secret")
    String webhookSecret;

    @POST
    @Path("/webhook")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response handleWebhook(@HeaderParam("Stripe-Signature") String signature, String payload) {
        try {
            // Vérifier la signature du webhook
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);

            LOG.info("Webhook reçu: " + event.getType());

            switch (event.getType()) {
                case "payment_intent.succeeded":
                    handlePaymentSucceeded(event);
                    break;
                case "payment_intent.payment_failed":
                    handlePaymentFailed(event);
                    break;
                case "payment_intent.canceled":
                    handlePaymentCanceled(event);
                    break;
                default:
                    LOG.info("Type d'événement non géré: " + event.getType());
            }

            return Response.ok().build();

        } catch (SignatureVerificationException e) {
            LOG.error("Signature de webhook invalide", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            LOG.error("Erreur lors du traitement du webhook", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void handlePaymentSucceeded(Event event) {
        try {
            com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
            
            if (paymentIntent != null) {
                rentalService.processPaymentCallback(paymentIntent.getId(), "succeeded");
                LOG.info("Paiement traité avec succès: " + paymentIntent.getId());
            }
        } catch (Exception e) {
            LOG.error("Erreur lors du traitement du paiement réussi", e);
        }
    }

    private void handlePaymentFailed(Event event) {
        try {
            com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
            
            if (paymentIntent != null) {
                rentalService.processPaymentCallback(paymentIntent.getId(), "failed");
                LOG.info("Paiement échoué: " + paymentIntent.getId());
            }
        } catch (Exception e) {
            LOG.error("Erreur lors du traitement du paiement échoué", e);
        }
    }

    private void handlePaymentCanceled(Event event) {
        try {
            com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
            
            if (paymentIntent != null) {
                rentalService.processPaymentCallback(paymentIntent.getId(), "canceled");
                LOG.info("Paiement annulé: " + paymentIntent.getId());
            }
        } catch (Exception e) {
            LOG.error("Erreur lors du traitement du paiement annulé", e);
        }
    }
}
