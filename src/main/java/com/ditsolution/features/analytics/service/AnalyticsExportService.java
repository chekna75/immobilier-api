package com.ditsolution.features.analytics.service;

import com.ditsolution.features.analytics.dto.AnalyticsReportDto;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@ApplicationScoped
public class AnalyticsExportService {

    private static final Logger LOG = Logger.getLogger(AnalyticsExportService.class);

    /**
     * Exporte un rapport en format CSV
     */
    public byte[] exportToCSV(AnalyticsReportDto report) {
        try {
            StringWriter writer = new StringWriter();
            
            // En-têtes
            writer.append("Métrique,Valeur\n");
            
            // Métriques de base
            writer.append("Vues totales,").append(String.valueOf(report.metrics.totalViews)).append("\n");
            writer.append("Vues uniques,").append(String.valueOf(report.metrics.uniqueViews)).append("\n");
            writer.append("Clics totaux,").append(String.valueOf(report.metrics.totalClicks)).append("\n");
            writer.append("Favoris totaux,").append(String.valueOf(report.metrics.totalFavorites)).append("\n");
            writer.append("Contacts totaux,").append(String.valueOf(report.metrics.totalContacts)).append("\n");
            writer.append("Conversions totales,").append(String.valueOf(report.metrics.totalConversions)).append("\n");
            writer.append("Taux de conversion,").append(String.format("%.2f%%", report.metrics.conversionRate)).append("\n");
            writer.append("Taux de clic,").append(String.format("%.2f%%", report.metrics.clickThroughRate)).append("\n");
            writer.append("Taux de favori,").append(String.format("%.2f%%", report.metrics.favoriteRate)).append("\n");
            writer.append("Taux de contact,").append(String.format("%.2f%%", report.metrics.contactRate)).append("\n");
            
            // Répartition par source
            writer.append("\nRépartition par source\n");
            if (report.breakdown.viewsBySource != null) {
                for (Map.Entry<String, Long> entry : report.breakdown.viewsBySource.entrySet()) {
                    writer.append(entry.getKey()).append(",").append(String.valueOf(entry.getValue())).append("\n");
                }
            }
            
            // Répartition par device
            writer.append("\nRépartition par device\n");
            if (report.breakdown.viewsByDevice != null) {
                for (Map.Entry<String, Long> entry : report.breakdown.viewsByDevice.entrySet()) {
                    writer.append(entry.getKey()).append(",").append(String.valueOf(entry.getValue())).append("\n");
                }
            }
            
            // Séries temporelles
            writer.append("\nSéries temporelles - Vues\n");
            writer.append("Date,Compte\n");
            if (report.timeSeries.views != null) {
                for (var point : report.timeSeries.views) {
                    writer.append(point.date).append(",").append(String.valueOf(point.count)).append("\n");
                }
            }
            
            return writer.toString().getBytes("UTF-8");
        } catch (IOException e) {
            LOG.error("Erreur lors de l'export CSV", e);
            throw new RuntimeException("Erreur lors de l'export CSV", e);
        }
    }

    /**
     * Exporte un rapport en format JSON
     */
    public byte[] exportToJSON(AnalyticsReportDto report) {
        try {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"reportId\": \"").append(report.id).append("\",\n");
            json.append("  \"generatedAt\": \"").append(report.generatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
            json.append("  \"period\": \"").append(report.period).append("\",\n");
            json.append("  \"reportType\": \"").append(report.reportType).append("\",\n");
            
            // Métriques
            json.append("  \"metrics\": {\n");
            json.append("    \"totalViews\": ").append(report.metrics.totalViews).append(",\n");
            json.append("    \"uniqueViews\": ").append(report.metrics.uniqueViews).append(",\n");
            json.append("    \"totalClicks\": ").append(report.metrics.totalClicks).append(",\n");
            json.append("    \"totalFavorites\": ").append(report.metrics.totalFavorites).append(",\n");
            json.append("    \"totalContacts\": ").append(report.metrics.totalContacts).append(",\n");
            json.append("    \"totalConversions\": ").append(report.metrics.totalConversions).append(",\n");
            json.append("    \"conversionRate\": ").append(report.metrics.conversionRate).append(",\n");
            json.append("    \"clickThroughRate\": ").append(report.metrics.clickThroughRate).append(",\n");
            json.append("    \"favoriteRate\": ").append(report.metrics.favoriteRate).append(",\n");
            json.append("    \"contactRate\": ").append(report.metrics.contactRate).append("\n");
            json.append("  },\n");
            
            // Résumé
            json.append("  \"summary\": {\n");
            json.append("    \"overview\": \"").append(escapeJson(report.summary.overview)).append("\",\n");
            json.append("    \"trends\": \"").append(escapeJson(report.summary.trends)).append("\",\n");
            json.append("    \"highlights\": [\n");
            if (report.summary.highlights != null) {
                for (int i = 0; i < report.summary.highlights.size(); i++) {
                    json.append("      \"").append(escapeJson(report.summary.highlights.get(i))).append("\"");
                    if (i < report.summary.highlights.size() - 1) {
                        json.append(",");
                    }
                    json.append("\n");
                }
            }
            json.append("    ]\n");
            json.append("  }\n");
            json.append("}");
            
            return json.toString().getBytes("UTF-8");
        } catch (Exception e) {
            LOG.error("Erreur lors de l'export JSON", e);
            throw new RuntimeException("Erreur lors de l'export JSON", e);
        }
    }

    /**
     * Exporte un rapport en format PDF (simulation)
     */
    public byte[] exportToPDF(AnalyticsReportDto report) {
        try {
            // Simulation d'un export PDF
            // Dans une vraie implémentation, utiliser une bibliothèque comme iText ou Apache PDFBox
            StringBuilder pdfContent = new StringBuilder();
            pdfContent.append("%PDF-1.4\n");
            pdfContent.append("1 0 obj\n");
            pdfContent.append("<< /Type /Catalog /Pages 2 0 R >>\n");
            pdfContent.append("endobj\n");
            pdfContent.append("2 0 obj\n");
            pdfContent.append("<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n");
            pdfContent.append("endobj\n");
            pdfContent.append("3 0 obj\n");
            pdfContent.append("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >>\n");
            pdfContent.append("endobj\n");
            pdfContent.append("4 0 obj\n");
            pdfContent.append("<< /Length 100 >>\n");
            pdfContent.append("stream\n");
            pdfContent.append("BT\n");
            pdfContent.append("/F1 12 Tf\n");
            pdfContent.append("50 700 Td\n");
            pdfContent.append("(Rapport Analytics - ").append(report.period).append(") Tj\n");
            pdfContent.append("ET\n");
            pdfContent.append("endstream\n");
            pdfContent.append("endobj\n");
            pdfContent.append("xref\n");
            pdfContent.append("0 5\n");
            pdfContent.append("0000000000 65535 f \n");
            pdfContent.append("0000000009 00000 n \n");
            pdfContent.append("0000000058 00000 n \n");
            pdfContent.append("0000000115 00000 n \n");
            pdfContent.append("0000000204 00000 n \n");
            pdfContent.append("trailer\n");
            pdfContent.append("<< /Size 5 /Root 1 0 R >>\n");
            pdfContent.append("startxref\n");
            pdfContent.append("304\n");
            pdfContent.append("%%EOF\n");
            
            return pdfContent.toString().getBytes("UTF-8");
        } catch (Exception e) {
            LOG.error("Erreur lors de l'export PDF", e);
            throw new RuntimeException("Erreur lors de l'export PDF", e);
        }
    }

    /**
     * Exporte un rapport en format Excel (simulation)
     */
    public byte[] exportToExcel(AnalyticsReportDto report) {
        try {
            // Simulation d'un export Excel
            // Dans une vraie implémentation, utiliser Apache POI
            StringBuilder excelContent = new StringBuilder();
            excelContent.append("<?xml version=\"1.0\"?>\n");
            excelContent.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\">\n");
            excelContent.append("<Worksheet ss:Name=\"Rapport Analytics\">\n");
            excelContent.append("<Table>\n");
            
            // En-têtes
            excelContent.append("<Row>\n");
            excelContent.append("<Cell><Data ss:Type=\"String\">Métrique</Data></Cell>\n");
            excelContent.append("<Cell><Data ss:Type=\"String\">Valeur</Data></Cell>\n");
            excelContent.append("</Row>\n");
            
            // Données
            excelContent.append("<Row>\n");
            excelContent.append("<Cell><Data ss:Type=\"String\">Vues totales</Data></Cell>\n");
            excelContent.append("<Cell><Data ss:Type=\"Number\">").append(report.metrics.totalViews).append("</Data></Cell>\n");
            excelContent.append("</Row>\n");
            
            excelContent.append("<Row>\n");
            excelContent.append("<Cell><Data ss:Type=\"String\">Vues uniques</Data></Cell>\n");
            excelContent.append("<Cell><Data ss:Type=\"Number\">").append(report.metrics.uniqueViews).append("</Data></Cell>\n");
            excelContent.append("</Row>\n");
            
            excelContent.append("<Row>\n");
            excelContent.append("<Cell><Data ss:Type=\"String\">Taux de conversion</Data></Cell>\n");
            excelContent.append("<Cell><Data ss:Type=\"Number\">").append(String.format("%.2f", report.metrics.conversionRate)).append("</Data></Cell>\n");
            excelContent.append("</Row>\n");
            
            excelContent.append("</Table>\n");
            excelContent.append("</Worksheet>\n");
            excelContent.append("</Workbook>\n");
            
            return excelContent.toString().getBytes("UTF-8");
        } catch (Exception e) {
            LOG.error("Erreur lors de l'export Excel", e);
            throw new RuntimeException("Erreur lors de l'export Excel", e);
        }
    }

    /**
     * Détermine le type MIME selon le format d'export
     */
    public String getContentType(String format) {
        return switch (format.toLowerCase()) {
            case "csv" -> "text/csv";
            case "json" -> "application/json";
            case "pdf" -> "application/pdf";
            case "excel", "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/octet-stream";
        };
    }

    /**
     * Génère un nom de fichier pour l'export
     */
    public String generateFileName(String reportType, String period, String format) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("analytics_%s_%s_%s.%s", reportType, period, timestamp, format);
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
