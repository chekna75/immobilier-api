package com.ditsolution.features.admin.service;

import com.ditsolution.features.admin.dto.AdminLogDto;
import com.ditsolution.features.auth.entity.AdminLogEntity;
import com.ditsolution.features.auth.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class AdminLogService {

    @PersistenceContext
    EntityManager em;

    public List<AdminLogDto> getLogs(int page, int size, String action, String targetType, UUID adminId) {
        StringBuilder query = new StringBuilder("SELECT al FROM AdminLogEntity al LEFT JOIN UserEntity u ON al.adminId = u.id WHERE 1=1");
        
        if (action != null && !action.trim().isEmpty()) {
            query.append(" AND al.action = :action");
        }
        if (targetType != null && !targetType.trim().isEmpty()) {
            query.append(" AND al.targetType = :targetType");
        }
        if (adminId != null) {
            query.append(" AND al.adminId = :adminId");
        }
        
        query.append(" ORDER BY al.createdAt DESC");

        var typedQuery = em.createQuery(query.toString(), AdminLogEntity.class);
        
        if (action != null && !action.trim().isEmpty()) {
            typedQuery.setParameter("action", action);
        }
        if (targetType != null && !targetType.trim().isEmpty()) {
            typedQuery.setParameter("targetType", targetType);
        }
        if (adminId != null) {
            typedQuery.setParameter("adminId", adminId);
        }

        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);

        List<AdminLogEntity> logs = typedQuery.getResultList();
        
        return logs.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public long getLogsCount(String action, String targetType, UUID adminId) {
        StringBuilder query = new StringBuilder("SELECT COUNT(al) FROM AdminLogEntity al WHERE 1=1");
        
        if (action != null && !action.trim().isEmpty()) {
            query.append(" AND al.action = :action");
        }
        if (targetType != null && !targetType.trim().isEmpty()) {
            query.append(" AND al.targetType = :targetType");
        }
        if (adminId != null) {
            query.append(" AND al.adminId = :adminId");
        }

        var typedQuery = em.createQuery(query.toString(), Long.class);
        
        if (action != null && !action.trim().isEmpty()) {
            typedQuery.setParameter("action", action);
        }
        if (targetType != null && !targetType.trim().isEmpty()) {
            typedQuery.setParameter("targetType", targetType);
        }
        if (adminId != null) {
            typedQuery.setParameter("adminId", adminId);
        }

        return typedQuery.getSingleResult();
    }

    public List<String> getAvailableActions() {
        String query = "SELECT DISTINCT al.action FROM AdminLogEntity al ORDER BY al.action";
        return em.createQuery(query, String.class).getResultList();
    }

    public List<String> getAvailableTargetTypes() {
        String query = "SELECT DISTINCT al.targetType FROM AdminLogEntity al WHERE al.targetType IS NOT NULL ORDER BY al.targetType";
        return em.createQuery(query, String.class).getResultList();
    }

    private AdminLogDto toDto(AdminLogEntity entity) {
        // Récupérer l'email de l'admin
        String adminEmail = null;
        if (entity.adminId != null) {
            UserEntity admin = UserEntity.findById(entity.adminId);
            if (admin != null) {
                adminEmail = admin.email;
            }
        }

        return new AdminLogDto(
            entity.id,
            entity.adminId,
            adminEmail,
            entity.action,
            entity.targetType,
            entity.targetId,
            entity.details,
            entity.ip,
            entity.userAgent,
            entity.createdAt
        );
    }
}
