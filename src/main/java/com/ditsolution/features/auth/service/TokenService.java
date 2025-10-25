package com.ditsolution.features.auth.service;

import com.ditsolution.features.auth.entity.RefreshTokenEntity;
import com.ditsolution.features.auth.entity.UserEntity;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TokenService {

    @ConfigProperty(name = "app.jwt.access.ttl.seconds", defaultValue = "900")
    int accessTtl; // 15 min
    @ConfigProperty(name = "app.jwt.refresh.ttl.days", defaultValue = "30")
    int refreshTtlDays; // 30 jours

    public String generateAccessToken(UserEntity user) {
        Set<String> groups = Set.of(user.role.name()); // TENANT/OWNER/ADMIN
        return Jwt.subject(user.id.toString())
                .issuer("immobilier-ci")                 // à mettre aussi côté verify
                .audience(Set.of("web", "mobile"))            // utile si tu veux vérifier côté clients
                .issuedAt(Instant.now())
                .expiresIn(15 * 60)                      // 15 min (configurable)
                .groups(groups)
                .claim("email", user.email)
                .claim("status", user.status.name())
                .claim("phone_verified", user.phoneVerified)
                .claim("email_verified", user.emailVerified)
                .sign();
    }

    @Transactional
    public RefreshTokenEntity issueRefreshToken(UserEntity user, String userAgent, String ipAddr, String tokenHash) {
        RefreshTokenEntity rt = new RefreshTokenEntity();
        // ⚠️ si @GeneratedValue est présent sur l’ID de l’entité, ne PAS setter rt.id à la main.
        rt.user = user;
        rt.tokenHash = tokenHash;           // hash SHA-256 du token brut côté service appelant
        rt.userAgent = userAgent;
        rt.ipAddr = ipAddr;
        rt.createdAt = OffsetDateTime.now();
        rt.expiresAt = rt.createdAt.plusDays(refreshTtlDays);
        rt.persist();
        return rt;
    }

    public boolean isRefreshValid(RefreshTokenEntity rt) {
        return rt != null && rt.expiresAt != null && rt.expiresAt.isAfter(OffsetDateTime.now()) && rt.revokedAt == null;
    }

    @Transactional
    public void revokeRefresh(RefreshTokenEntity rt) {
        if (rt != null && rt.revokedAt == null) {
            rt.revokedAt = OffsetDateTime.now();
        }
    }

    @Transactional
    public RefreshTokenEntity rotateRefresh(RefreshTokenEntity oldRt, String newTokenHash, String newUserAgent, String newIp) {
    if (!isRefreshValid(oldRt)) {
        throw new IllegalStateException("Invalid refresh");
    }
    // Révoque l’ancien
    oldRt.revokedAt = OffsetDateTime.now();

    // Émet le nouveau
    RefreshTokenEntity newRt = new RefreshTokenEntity();
    newRt.user = oldRt.user;
    newRt.tokenHash = newTokenHash;
    newRt.userAgent = newUserAgent;
    newRt.ipAddr = newIp;
    newRt.createdAt = OffsetDateTime.now();
    newRt.expiresAt = oldRt.createdAt.plusDays(refreshTtlDays);
    newRt.persist();

    return newRt;
}

    public String generateImpersonationToken(UserEntity targetUser) {
        Set<String> groups = Set.of(targetUser.role.name());
        return Jwt.subject(targetUser.id.toString())
                .issuer("immobilier-ci")
                .audience(Set.of("web", "mobile"))
                .issuedAt(Instant.now())
                .expiresIn(60 * 60)  // 1 heure pour l'impersonation
                .groups(groups)
                .claim("email", targetUser.email)
                .claim("status", targetUser.status.name())
                .claim("phone_verified", targetUser.phoneVerified)
                .claim("email_verified", targetUser.emailVerified)
                .claim("impersonated", true)  // Marquer comme impersonation
                .sign();
    }
}


