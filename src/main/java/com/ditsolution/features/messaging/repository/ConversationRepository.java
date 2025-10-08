package com.ditsolution.features.messaging.repository;

import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.listing.entity.ListingEntity;
import com.ditsolution.features.messaging.entity.ConversationEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ConversationRepository implements PanacheRepository<ConversationEntity> {
    
    /**
     * Trouve une conversation existante entre un locataire et un propriétaire pour une propriété donnée
     */
    public Optional<ConversationEntity> findByPropertyAndUsers(ListingEntity property, UserEntity tenant, UserEntity owner) {
        return find("property = ?1 AND tenant = ?2 AND owner = ?3 AND isActive = true", property, tenant, owner).firstResultOptional();
    }
    
    /**
     * Trouve une conversation par propriété et utilisateur (peu importe le rôle)
     */
    public Optional<ConversationEntity> findByPropertyAndUser(ListingEntity property, UserEntity user) {
        return find("property = ?1 AND (tenant = ?2 OR owner = ?2) AND isActive = true", property, user).firstResultOptional();
    }
    
    /**
     * Récupère toutes les conversations d'un utilisateur
     */
    public List<ConversationEntity> findByUser(UserEntity user) {
        return find("(tenant = ?1 OR owner = ?1) AND isActive = true ORDER BY lastMessageTime DESC", user).list();
    }
    
    /**
     * Récupère les conversations non archivées d'un utilisateur
     */
    public List<ConversationEntity> findActiveByUser(UserEntity user) {
        return find("(tenant = ?1 OR owner = ?1) AND isActive = true AND isArchived = false ORDER BY lastMessageTime DESC", user).list();
    }
    
    /**
     * Compte le nombre de messages non lus pour un utilisateur
     */
    public Integer countUnreadMessagesForUser(UserEntity user) {
        return find("(tenant = ?1 OR owner = ?1) AND isActive = true", user)
            .stream()
            .mapToInt(c -> c.getTenant().equals(user) ? c.getTenantUnreadCount() : c.getOwnerUnreadCount())
            .sum();
    }
    
    /**
     * Trouve une conversation par ID et utilisateur (vérification des permissions)
     */
    public Optional<ConversationEntity> findByIdAndUser(Long conversationId, UserEntity user) {
        return find("id = ?1 AND (tenant = ?2 OR owner = ?2) AND isActive = true", conversationId, user).firstResultOptional();
    }
    
    /**
     * Récupère les conversations récentes (dernières 30 jours)
     */
    public List<ConversationEntity> findRecentByUser(UserEntity user, java.time.LocalDateTime since) {
        return find("(tenant = ?1 OR owner = ?1) AND isActive = true AND lastMessageTime >= ?2 ORDER BY lastMessageTime DESC", user, since).list();
    }
    
    /**
     * Recherche des conversations par nom d'utilisateur ou titre de propriété
     */
    public List<ConversationEntity> searchByUserAndTerm(UserEntity user, String searchTerm) {
        return find("(tenant = ?1 OR owner = ?1) AND isActive = true AND " +
                   "(LOWER(tenant.firstName || ' ' || tenant.lastName) LIKE LOWER(?2) OR " +
                   "LOWER(owner.firstName || ' ' || owner.lastName) LIKE LOWER(?2) OR " +
                   "LOWER(property.title) LIKE LOWER(?2)) " +
                   "ORDER BY lastMessageTime DESC", user, "%" + searchTerm + "%").list();
    }
    
    // ===== MÉTHODES D'ADMINISTRATION =====
    
    /**
     * Recherche toutes les conversations (pour les administrateurs)
     */
    public List<ConversationEntity> searchAllConversations(String searchTerm) {
        return find("(LOWER(tenant.firstName || ' ' || tenant.lastName) LIKE LOWER(?1) OR " +
                   "LOWER(owner.firstName || ' ' || owner.lastName) LIKE LOWER(?1) OR " +
                   "LOWER(property.title) LIKE LOWER(?1)) " +
                   "ORDER BY lastMessageTime DESC", "%" + searchTerm + "%").list();
    }
    
    /**
     * Trouve les conversations par statut d'archivage (pour les administrateurs)
     */
    public List<ConversationEntity> findByArchivedStatus(Boolean archived) {
        return find("isArchived = ?1 ORDER BY lastMessageTime DESC", archived).list();
    }
}
