package com.ditsolution.features.messaging.entity;

import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.listing.entity.ListingEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversations")
public class ConversationEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private ListingEntity property;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private UserEntity tenant;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;
    
    @Column(name = "last_message")
    private String lastMessage;
    
    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime;
    
    @Column(name = "tenant_unread_count", nullable = false)
    private Integer tenantUnreadCount = 0;
    
    @Column(name = "owner_unread_count", nullable = false)
    private Integer ownerUnreadCount = 0;
    
    @Column(name = "is_archived", nullable = false)
    private Boolean isArchived = false;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<MessageEntity> messages = new ArrayList<>();
    
    // Constructeurs
    public ConversationEntity() {}
    
    public ConversationEntity(ListingEntity property, UserEntity tenant, UserEntity owner) {
        this.property = property;
        this.tenant = tenant;
        this.owner = owner;
    }
    
    // Getters et Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public ListingEntity getProperty() {
        return property;
    }
    
    public void setProperty(ListingEntity property) {
        this.property = property;
    }
    
    public UserEntity getTenant() {
        return tenant;
    }
    
    public void setTenant(UserEntity tenant) {
        this.tenant = tenant;
    }
    
    public UserEntity getOwner() {
        return owner;
    }
    
    public void setOwner(UserEntity owner) {
        this.owner = owner;
    }
    
    public String getLastMessage() {
        return lastMessage;
    }
    
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
    
    public LocalDateTime getLastMessageTime() {
        return lastMessageTime;
    }
    
    public void setLastMessageTime(LocalDateTime lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }
    
    public Integer getTenantUnreadCount() {
        return tenantUnreadCount;
    }
    
    public void setTenantUnreadCount(Integer tenantUnreadCount) {
        this.tenantUnreadCount = tenantUnreadCount;
    }
    
    public Integer getOwnerUnreadCount() {
        return ownerUnreadCount;
    }
    
    public void setOwnerUnreadCount(Integer ownerUnreadCount) {
        this.ownerUnreadCount = ownerUnreadCount;
    }
    
    public Boolean getIsArchived() {
        return isArchived;
    }
    
    public void setIsArchived(Boolean isArchived) {
        this.isArchived = isArchived;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public List<MessageEntity> getMessages() {
        return messages;
    }
    
    public void setMessages(List<MessageEntity> messages) {
        this.messages = messages;
    }
    
    // Méthodes utilitaires
    public void incrementUnreadCount(UserEntity user) {
        if (user.equals(tenant)) {
            this.tenantUnreadCount++;
        } else if (user.equals(owner)) {
            this.ownerUnreadCount++;
        }
    }
    
    public void resetUnreadCount(UserEntity user) {
        if (user.equals(tenant)) {
            this.tenantUnreadCount = 0;
        } else if (user.equals(owner)) {
            this.ownerUnreadCount = 0;
        }
    }
    
    public Integer getUnreadCountForUser(UserEntity user) {
        if (user.equals(tenant)) {
            return this.tenantUnreadCount;
        } else if (user.equals(owner)) {
            return this.ownerUnreadCount;
        }
        return 0;
    }
    
    public UserEntity getOtherUser(UserEntity currentUser) {
        if (currentUser.equals(tenant)) {
            return owner;
        } else if (currentUser.equals(owner)) {
            return tenant;
        }
        return null;
    }
}
