package com.ditsolution.features.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import java.time.OffsetDateTime;

// User.java
@Entity
@Data
@EqualsAndHashCode(callSuper=false)
@Table(name = "users")
public class UserEntity extends PanacheEntityBase {
  @Id @GeneratedValue
  public UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public Role role = Role.TENANT;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public Status status = Status.ACTIVE;

  public String email;
  @Column(name="phone_e164")
  public String phoneE164;
  @Column(name = "phoneVerified", nullable = false, columnDefinition = "boolean DEFAULT false")
  public boolean phoneVerified = false;
  @Column(name = "emailVerified", nullable = false, columnDefinition = "boolean DEFAULT false")
  public boolean emailVerified = false;

  public String passwordHash;
  public String firstName;
  public String lastName;
  public String avatarUrl;

  public OffsetDateTime createdAt;
  public OffsetDateTime updatedAt;

  @PrePersist
  void onCreate() { createdAt = OffsetDateTime.now(); updatedAt = createdAt; }

  @PreUpdate
  void onUpdate() { updatedAt = OffsetDateTime.now(); }

  public enum Role { TENANT, OWNER, ADMIN }
  public enum Status { ACTIVE, SUSPENDED }
}

