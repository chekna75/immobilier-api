package com.ditsolution.common.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;

@ApplicationScoped
public class EmailService {

    @ConfigProperty(name = "quarkus.mailer.host", defaultValue = "")
    String host;

    @ConfigProperty(name = "quarkus.mailer.port", defaultValue = "587")
    int port;

    @ConfigProperty(name = "quarkus.mailer.username", defaultValue = "")
    String username;

    @ConfigProperty(name = "quarkus.mailer.password", defaultValue = "")
    String password;

    @ConfigProperty(name = "quarkus.mailer.ssl", defaultValue = "false")
    boolean ssl;

    @ConfigProperty(name = "quarkus.mailer.start-tls", defaultValue = "true")
    boolean startTls;

    @Inject
    private Mailer mailer;

    public void sendEmail(String to, String username) {
        mailer.send(Mail.withText(to, "Bienvenue sur AmbuConnect 🎉",
            "Bonjour " + username + ",\n\n" +
            "Merci pour ton inscription !\n" +
            "Tu peux dès maintenant publier et gérer tes annonces.\n\n" +
            "L'équipe D IT Solution 🚀"
        ));
    }

    public void sendListingPublishedEmail(String to, String title) {
        mailer.send(Mail.withText(
            to,
            "Ton annonce a été publiée ✅",
            "Bonjour,\n\n" +
            "Ton annonce \"" + title + "\" est maintenant en ligne !\n" +
            "Bonne chance pour tes recherches.\n\n" +
            "L'équipe D IT Solution 🚀"
        ));
    }
    
}
