package com.ditsolution.features.rental.service;

import com.ditsolution.features.rental.entity.RentPaymentEntity;
import com.ditsolution.features.rental.entity.SplitPaymentItemEntity;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class PdfGenerationService {

    private static final Logger LOG = Logger.getLogger(PdfGenerationService.class);

    @ConfigProperty(name = "app.base.url", defaultValue = "http://localhost:8080")
    String baseUrl;

    public String generateReceipt(RentPaymentEntity payment) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // En-tête avec logo
            document.add(new Paragraph("REÇU DE PAIEMENT")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(20)
                    .setBold()
                    .setMarginBottom(10));

            document.add(new Paragraph("App Immo - Gestion Locative")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(14)
                    .setMarginBottom(30));

            // Numéro de reçu et date
            Table headerTable = new Table(2).useAllAvailableWidth();
            headerTable.addCell("Numéro de reçu:");
            headerTable.addCell("RCP-" + payment.id);
            headerTable.addCell("Date d'émission:");
            headerTable.addCell(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            
            document.add(headerTable);
            document.add(new Paragraph("\n"));

            // Informations du paiement
            Table paymentTable = new Table(2).useAllAvailableWidth();
            
            paymentTable.addCell("Date de paiement:");
            paymentTable.addCell(payment.getPaidDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            
            paymentTable.addCell("Montant payé:");
            paymentTable.addCell(payment.getAmount() + " EUR");
            
            paymentTable.addCell("Méthode de paiement:");
            paymentTable.addCell(payment.getPaymentMethod());
            
            paymentTable.addCell("Référence transaction:");
            paymentTable.addCell(payment.getCinetpayTransactionId() != null ?
                payment.getCinetpayTransactionId() : "Non disponible");

            document.add(paymentTable);

            // Informations du contrat
            Table contractTable = new Table(2).useAllAvailableWidth();
            contractTable.addCell("Contrat:");
            contractTable.addCell(payment.getContract().getContractNumber());
            contractTable.addCell("Propriété:");
            contractTable.addCell(payment.getContract().getProperty().getTitle());
            contractTable.addCell("Locataire:");
            contractTable.addCell(payment.getContract().getTenant().getFirstName() + 
                " " + payment.getContract().getTenant().getLastName());
            contractTable.addCell("Propriétaire:");
            contractTable.addCell(payment.getContract().getOwner().getFirstName() + 
                " " + payment.getContract().getOwner().getLastName());

            document.add(contractTable);

            // Signature et validation
            document.add(new Paragraph("\n\nVALIDATION DU PAIEMENT")
                    .setFontSize(14)
                    .setBold()
                    .setMarginTop(30)
                    .setMarginBottom(10));

            document.add(new Paragraph("Ce reçu certifie que le paiement du loyer a été effectué avec succès.")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(12)
                    .setMarginBottom(20));

            // Signature numérique
            Table signatureTable = new Table(2).useAllAvailableWidth();
            signatureTable.addCell("Signature numérique:");
            signatureTable.addCell("✓ Paiement validé automatiquement");
            signatureTable.addCell("Date de validation:");
            signatureTable.addCell(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
            
            document.add(signatureTable);

            // Pied de page
            document.add(new Paragraph("\n\nCe document est généré automatiquement par le système App Immo.")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10)
                    .setMarginTop(20));

            document.add(new Paragraph("Pour toute question, contactez le support client.")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10));

            document.close();

            // Ici, vous devriez sauvegarder le PDF et retourner l'URL
            // Pour simplifier, on retourne une URL fictive
            String receiptUrl = baseUrl + "/receipts/receipt-" + payment.id + ".pdf";
            
            LOG.info("Reçu PDF généré pour le paiement: " + payment.id);
            return receiptUrl;

        } catch (Exception e) {
            LOG.error("Erreur lors de la génération du PDF", e);
            throw new RuntimeException("Erreur lors de la génération du reçu", e);
        }
    }

    /**
     * Générer un reçu pour un élément de paiement fractionné
     */
    public String generateSplitPaymentReceipt(SplitPaymentItemEntity item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // En-tête avec logo
            String title = item.getPaymentType() == SplitPaymentItemEntity.PaymentType.DEPOSIT ? 
                "REÇU D'ACOMPTE" : "REÇU DE SOLDE";
            
            document.add(new Paragraph(title)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(20)
                    .setBold()
                    .setMarginBottom(10));

            document.add(new Paragraph("App Immo - Gestion Locative")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(14)
                    .setMarginBottom(30));

            // Numéro de reçu et date
            Table headerTable = new Table(2).useAllAvailableWidth();
            headerTable.addCell("Numéro de reçu:");
            headerTable.addCell("SPR-" + item.id);
            headerTable.addCell("Date d'émission:");
            headerTable.addCell(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            
            document.add(headerTable);
            document.add(new Paragraph("\n"));

            // Informations du paiement fractionné
            Table paymentTable = new Table(2).useAllAvailableWidth();
            
            paymentTable.addCell("Type de paiement:");
            paymentTable.addCell(item.getPaymentType() == SplitPaymentItemEntity.PaymentType.DEPOSIT ? 
                "Acompte" : "Solde");
            
            paymentTable.addCell("Date de paiement:");
            paymentTable.addCell(item.getPaidDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            
            paymentTable.addCell("Montant payé:");
            paymentTable.addCell(item.getAmount() + " EUR");
            
            paymentTable.addCell("Méthode de paiement:");
            paymentTable.addCell(item.getPaymentMethod() != null ? item.getPaymentMethod() : "Non spécifiée");
            
            paymentTable.addCell("Référence transaction:");
            paymentTable.addCell(item.getStripePaymentIntentId() != null ? 
                item.getStripePaymentIntentId() : "Non disponible");

            document.add(paymentTable);

            // Informations du contrat
            Table contractTable = new Table(2).useAllAvailableWidth();
            contractTable.addCell("Contrat:");
            contractTable.addCell(item.getSplitPayment().getContract().getContractNumber());
            contractTable.addCell("Propriété:");
            contractTable.addCell(item.getSplitPayment().getContract().getProperty().getTitle());
            contractTable.addCell("Locataire:");
            contractTable.addCell(item.getSplitPayment().getContract().getTenant().getFirstName() + 
                " " + item.getSplitPayment().getContract().getTenant().getLastName());
            contractTable.addCell("Propriétaire:");
            contractTable.addCell(item.getSplitPayment().getContract().getOwner().getFirstName() + 
                " " + item.getSplitPayment().getContract().getOwner().getLastName());

            document.add(contractTable);

            // Signature et validation
            document.add(new Paragraph("\n\nVALIDATION DU PAIEMENT")
                    .setFontSize(14)
                    .setBold()
                    .setMarginTop(30)
                    .setMarginBottom(10));

            document.add(new Paragraph("Ce reçu certifie que le paiement fractionné a été effectué avec succès.")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(12)
                    .setMarginBottom(20));

            // Signature numérique
            Table signatureTable = new Table(2).useAllAvailableWidth();
            signatureTable.addCell("Signature numérique:");
            signatureTable.addCell("✓ Paiement validé automatiquement");
            signatureTable.addCell("Date de validation:");
            signatureTable.addCell(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
            
            document.add(signatureTable);

            // Pied de page
            document.add(new Paragraph("\n\nCe document est généré automatiquement par le système App Immo.")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10)
                    .setMarginTop(20));

            document.add(new Paragraph("Pour toute question, contactez le support client.")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10));

            document.close();

            // Retourner l'URL du reçu
            String receiptUrl = baseUrl + "/receipts/split-receipt-" + item.id + ".pdf";
            
            LOG.info("Reçu PDF généré pour l'élément de paiement fractionné: " + item.id);
            return receiptUrl;

        } catch (Exception e) {
            LOG.error("Erreur lors de la génération du PDF pour paiement fractionné", e);
            throw new RuntimeException("Erreur lors de la génération du reçu", e);
        }
    }
}