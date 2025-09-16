package com.ditsolution.features.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.PrePersist;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;


import java.time.OffsetDateTime;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

// RefreshToken.java
@Entity
@Table(name="refresh_tokens")
@Data
@EqualsAndHashCode(callSuper = false)
public class RefreshTokenEntity extends PanacheEntityBase {
  @Id @GeneratedValue
  public UUID id;

  @ManyToOne(optional=false)
  @JoinColumn(name="user_id")
  public UserEntity user;

  public String tokenHash;
  public String userAgent;
  public String ipAddr;

  public OffsetDateTime createdAt;
  public OffsetDateTime expiresAt;
  public OffsetDateTime revokedAt;

  @PrePersist
  void onCreate() { createdAt = OffsetDateTime.now(); }
}

