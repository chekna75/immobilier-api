package com.ditsolution.features.admin.service;

import com.ditsolution.features.admin.dto.AdminUserDto;
import com.ditsolution.features.admin.dto.AdminUserFilterDto;
import com.ditsolution.features.admin.dto.AdminUserUpdateDto;
import com.ditsolution.features.admin.dto.AdminImpersonateDto;
import com.ditsolution.features.auth.entity.AdminLogEntity;
import com.ditsolution.features.admin.entity.ImpersonationTokenEntity;
import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.auth.service.TokenService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class AdminUserService {

    @Inject
    TokenService tokenService;

    @Inject
    JsonWebToken jwt;

    @Inject
    EntityManager entityManager;

    @Transactional
    public List<AdminUserDto> getUsers(AdminUserFilterDto filter) {
        StringBuilder query = new StringBuilder("SELECT u FROM UserEntity u WHERE 1=1");
        
        if (filter.email() != null && !filter.email().isBlank()) {
            query.append(" AND LOWER(u.email) LIKE LOWER(:email)");
        }
        if (filter.role() != null && !filter.role().isBlank()) {
            query.append(" AND u.role = :role");
        }
        if (filter.status() != null && !filter.status().isBlank()) {
            query.append(" AND u.status = :status");
        }
        
        query.append(" ORDER BY u.").append(filter.sortBy()).append(" ").append(filter.sortDirection());
        
        var queryObj = UserEntity.getEntityManager().createQuery(query.toString(), UserEntity.class);
        
        if (filter.email() != null && !filter.email().isBlank()) {
            queryObj.setParameter("email", "%" + filter.email() + "%");
        }
        if (filter.role() != null && !filter.role().isBlank()) {
            queryObj.setParameter("role", UserEntity.Role.valueOf(filter.role()));
        }
        if (filter.status() != null && !filter.status().isBlank()) {
            queryObj.setParameter("status", UserEntity.Status.valueOf(filter.status()));
        }
        
        List<UserEntity> users = queryObj
            .setFirstResult(filter.page() * filter.size())
            .setMaxResults(filter.size())
            .getResultList();
        
        return users.stream().map(this::mapToAdminUserDto).collect(Collectors.toList());
    }
    
    @Transactional
    public AdminUserDto getUserById(UUID userId) {
        UserEntity user = UserEntity.findById(userId);
        if (user == null) {
            throw new RuntimeException("Utilisateur non trouvé");
        }
        return mapToAdminUserDto(user);
    }
    
    @Transactional
    public void updateUser(AdminUserUpdateDto updateDto) {
        UserEntity user = UserEntity.findById(updateDto.userId());
        if (user == null) {
            throw new RuntimeException("Utilisateur non trouvé");
        }
        
        // Log de l'action admin
        logAdminAction("USER_UPDATE", updateDto.userId().toString(), updateDto.reason());
        
        if (updateDto.role() != null && !updateDto.role().isBlank()) {
            user.role = UserEntity.Role.valueOf(updateDto.role());
        }
        if (updateDto.status() != null && !updateDto.status().isBlank()) {
            user.status = UserEntity.Status.valueOf(updateDto.status());
        }
        
        user.persist();
    }
    
    @Transactional
    public String impersonateUser(AdminImpersonateDto impersonateDto) {
        UserEntity targetUser = UserEntity.findById(impersonateDto.targetUserId());
        if (targetUser == null) {
            throw new RuntimeException("Utilisateur cible non trouvé");
        }
        
        UUID adminId = UUID.fromString(jwt.getSubject());
        UserEntity admin = UserEntity.findById(adminId);
        
        // Log de l'action admin
        logAdminAction("IMPERSONATE", impersonateDto.targetUserId().toString(), impersonateDto.reason());
        
        // Créer un token d'impersonation
        String token = tokenService.generateImpersonationToken(targetUser);
        
        // Enregistrer le token d'impersonation
        ImpersonationTokenEntity impersonationToken = new ImpersonationTokenEntity();
        impersonationToken.admin = admin;
        impersonationToken.targetUser = targetUser;
        try {
            impersonationToken.tokenHash = java.security.MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes()).toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("Erreur lors de la génération du hash du token", e);
        }
        impersonationToken.reason = impersonateDto.reason();
        impersonationToken.expiresAt = java.time.OffsetDateTime.now().plusHours(1);
        impersonationToken.persist();
        
        return token;
    }
    
    private AdminUserDto mapToAdminUserDto(UserEntity user) {
        long listingsCount = entityManager.createQuery("SELECT COUNT(l) FROM ListingEntity l WHERE l.owner.id = :userId", Long.class)
            .setParameter("userId", user.id).getSingleResult();
        
        return new AdminUserDto(
            user.id,
            user.email,
            user.firstName,
            user.lastName,
            user.phoneE164,
            user.phoneVerified,
            user.emailVerified,
            user.role.name(),
            user.status.name(),
            user.createdAt,
            user.updatedAt,
            user.avatarUrl,
            listingsCount
        );
    }
    
    private void logAdminAction(String action, String targetId, String reason) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        
        AdminLogEntity log = new AdminLogEntity();
        log.adminId = adminId;
        log.action = action;
        log.targetType = "USER";
        log.targetId = UUID.fromString(targetId);
        log.details = createDetailsJson(reason);
        log.persist();
    }
    
    private com.fasterxml.jackson.databind.JsonNode createDetailsJson(String reason) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.createObjectNode().put("reason", reason);
        } catch (Exception e) {
            return null;
        }
    }
}
