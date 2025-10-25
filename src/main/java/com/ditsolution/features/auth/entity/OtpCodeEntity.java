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
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Column;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


// OtpCode.java
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name="otp_codes")
public class OtpCodeEntity extends PanacheEntityBase {
  @Id @GeneratedValue
  public UUID id;

  @ManyToOne(optional=false)
  @JoinColumn(name="user_id")
  public UserEntity user;

  public String code;

  @Enumerated(EnumType.STRING)
  public Channel channel;   // SMS, EMAIL
  @Enumerated(EnumType.STRING)
  public Purpose purpose;   // LOGIN, VERIFY_PHONE, RESET_PASSWORD

  public OffsetDateTime expiresAt;
  public OffsetDateTime usedAt;
  public int attemptCount;
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  public String meta;

  public OffsetDateTime createdAt;

  @PrePersist
  void onCreate() { createdAt = OffsetDateTime.now(); }

  public enum Channel { SMS, EMAIL }
  public enum Purpose { LOGIN, VERIFY_PHONE, RESET_PASSWORD }
}
