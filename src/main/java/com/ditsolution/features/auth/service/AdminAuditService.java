package com.ditsolution.features.auth.service;

import com.ditsolution.features.auth.entity.AdminLogEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

@ApplicationScoped
public class AdminAuditService {

    public static final String ACTION_USER_SUSPEND = "USER_SUSPEND";
    public static final String ACTION_USER_ACTIVATE = "USER_ACTIVATE";
    public static final String ACTION_LISTING_REMOVE = "LISTING_REMOVE";
    public static final String ACTION_ROLE_APPROVE = "ROLE_APPROVE";
    public static final String ACTION_ROLE_REJECT = "ROLE_REJECT";

    @Transactional
    public void log(
            UUID adminId,
            String action,
            String targetType,
            UUID targetId,
            String details,
            String ip,
            String userAgent
    ) {
        AdminLogEntity entry = new AdminLogEntity();
        entry.adminId = adminId;
        entry.action = action;
        entry.targetType = targetType;
        entry.targetId = targetId;
        
        // Convertir la string en JsonNode
        try {
            ObjectMapper mapper = new ObjectMapper();
            entry.details = mapper.readTree(details);
        } catch (Exception e) {
            // Fallback: créer un objet JSON simple
            try {
                ObjectMapper mapper = new ObjectMapper();
                entry.details = mapper.createObjectNode().put("message", details);
            } catch (Exception ex) {
                // Dernier recours: null
                entry.details = null;
            }
        }
        
        entry.ip = ip;
        entry.userAgent = userAgent;
        entry.persist();
    }

    // Méthode simplifiée pour les actions de changement de rôle
    @Transactional
    public void log(String action, String details, UUID targetId, String ip, String userAgent) {
        // Pour les actions de changement de rôle, on utilise l'ID de l'utilisateur cible comme adminId
        // Dans un vrai système, on récupérerait l'ID de l'admin connecté
        log(targetId, action, "USER", targetId, details, ip, userAgent);
    }
}
