package com.ditsolution.features.admin.entity;

import com.ditsolution.features.auth.entity.UserEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "impersonation_tokens")
public class ImpersonationTokenEntity extends PanacheEntityBase {
    
    @Id
    @GeneratedValue
    public Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    public UserEntity admin;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    public UserEntity targetUser;
    
    @Column(name = "token_hash", nullable = false)
    public String tokenHash;
    
    public String reason;
    
    @Column(name = "expires_at", nullable = false)
    public OffsetDateTime expiresAt;
    
    @Column(name = "used_at")
    public OffsetDateTime usedAt;
    
    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt = OffsetDateTime.now();
}
