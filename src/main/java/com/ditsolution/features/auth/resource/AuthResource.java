package com.ditsolution.features.auth.resource;

import com.ditsolution.common.services.EmailService;
import com.ditsolution.features.auth.dto.AuthDtos.*;
import com.ditsolution.features.auth.entity.OtpCodeEntity;
import com.ditsolution.features.auth.entity.RefreshTokenEntity;
import com.ditsolution.features.auth.entity.RoleChangeRequestEntity;
import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.auth.mapper.AuthMappers;
import com.ditsolution.features.auth.service.PasswordService;
import com.ditsolution.features.auth.service.PhoneService;
import com.ditsolution.features.auth.service.TokenService;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequestScoped
@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject PasswordService passwordService;
    @Inject TokenService tokenService;
    @Inject PhoneService phoneService;
    @Inject SecurityIdentity identity;
    @Inject EmailService emailService;
    
    @POST
    @Path("/register")
    @Transactional
    public Response register(RegisterRequest req, @HeaderParam("User-Agent") String ua, @HeaderParam("X-Forwarded-For") String ipRaw) {
        if (req == null || req.email() == null || req.password() == null
            || req.email().isBlank() || req.password().isBlank()) {
            return badRequest("email et password sont obligatoires");
        }
    
        String email = req.email().trim().toLowerCase();
        if (UserEntity.find("email", email).firstResult() != null) {
            return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorDto("EMAIL_TAKEN", "Email déjà utilisé")).build();
        }
    
        UserEntity user = new UserEntity();
        user.email = email;
        user.passwordHash = passwordService.hash(req.password());
        user.firstName = req.firstName();
        user.lastName = req.lastName();
        if (req.phone()!=null) {
            String e164 = phoneService.normalizeE164(req.phone());
            if (e164 == null) return badRequest("Numéro de téléphone invalide");
            user.phoneE164 = e164;
        }
        // Assigner le rôle demandé ou TENANT par défaut
        if (req.role() != null && !req.role().isBlank()) {
            try {
                user.role = UserEntity.Role.valueOf(req.role().toUpperCase());
            } catch (IllegalArgumentException e) {
                user.role = UserEntity.Role.TENANT; // Rôle invalide, utiliser TENANT par défaut
            }
        } else {
            user.role = UserEntity.Role.TENANT; // Pas de rôle spécifié, utiliser TENANT par défaut
        }
        user.status = UserEntity.Status.ACTIVE;
        user.persist();
    
        String access = tokenService.generateAccessToken(user);
        String rawRefresh = UUID.randomUUID().toString();
        String refreshHash = passwordService.hash(rawRefresh);
        tokenService.issueRefreshToken(user, ua, firstIp(ipRaw), refreshHash);

        emailService.sendEmail(user.email, user.firstName);
    
        return Response.status(Response.Status.CREATED)
            .entity(new AuthResponse(access, rawRefresh, AuthMappers.toDto(user))).build();
    }
    
    private Response badRequest(String msg){
      return Response.status(Response.Status.BAD_REQUEST)
        .entity(new ErrorDto("VALIDATION_ERROR", msg)).build();
    }
    record ErrorDto(String error, String message) {}
    

    @POST
    @Path("/login-email")
    @Transactional
    public Response loginEmail(LoginEmailRequest req, @HeaderParam("User-Agent") String ua, @HeaderParam("X-Forwarded-For") String ip) {
        UserEntity user = UserEntity.find("email", req.email()).firstResult();
        if (user == null || user.passwordHash == null || !passwordService.matches(req.password(), user.passwordHash)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorDto("INVALID_CREDENTIALS", "Email ou mot de passe incorrect")).build();
        }
        String access = tokenService.generateAccessToken(user);
        String rawRefresh = UUID.randomUUID().toString();
        String refreshHash = passwordService.hash(rawRefresh);
        tokenService.issueRefreshToken(user, ua, ip, refreshHash);
        return Response.ok(new AuthResponse(access, rawRefresh, AuthMappers.toDto(user))).build();
    }

    @POST
    @Path("/request-otp")
    @Transactional
    public Response requestOtp(RequestOtpRequest req) {
        String phone = phoneService.normalizeE164(req.phone());
        if (phone == null || phone.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorDto("VALIDATION_ERROR", "Numéro de téléphone requis")).build();
        }
        UserEntity user = UserEntity.find("phoneE164", phone).firstResult();
        if (user == null) {
            user = new UserEntity();
            user.id = UUID.randomUUID();
            user.phoneE164 = phone;
            user.role = UserEntity.Role.TENANT;
            user.status = UserEntity.Status.ACTIVE;
            user.persist();
        }
        // cooldown 60s
        OffsetDateTime now = OffsetDateTime.now();
        OtpCodeEntity last = OtpCodeEntity.find("user = ?1 order by createdAt desc", user).firstResult();
        if (last != null && last.createdAt != null && ChronoUnit.SECONDS.between(last.createdAt, now) < 60) {
            return Response.status(Response.Status.TOO_MANY_REQUESTS)
                .entity(new ErrorDto("RATE_LIMIT", "Veuillez attendre 60 secondes avant de demander un nouveau code")).build();
        }
        // max 5/jour
        OffsetDateTime startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        long countToday = OtpCodeEntity.find("user = ?1 and createdAt >= ?2", user, startOfDay).count();
        if (countToday >= 5) {
            return Response.status(Response.Status.TOO_MANY_REQUESTS)
                .entity(new ErrorDto("RATE_LIMIT", "Maximum 5 codes par jour atteint")).build();
        }
        String code = String.format("%06d", (int)(Math.random() * 1_000_000));
        OtpCodeEntity otp = new OtpCodeEntity();
        otp.id = UUID.randomUUID();
        otp.user = user;
        otp.code = code;
        otp.channel = OtpCodeEntity.Channel.SMS;
        otp.purpose = OtpCodeEntity.Purpose.LOGIN;
        otp.expiresAt = now.plusMinutes(5);
        otp.attemptCount = 0;
        otp.createdAt = now;
        otp.persist();
        // TODO: intégrer envoi via provider SMS
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @POST
    @Path("/login-otp")
    @Transactional
    public Response loginOtp(LoginOtpRequest req, @HeaderParam("User-Agent") String ua, @HeaderParam("X-Forwarded-For") String ip) {
        String phone = phoneService.normalizeE164(req.phone());
        UserEntity user = UserEntity.find("phoneE164", phone).firstResult();
        if (user == null) return Response.status(Response.Status.UNAUTHORIZED)
            .entity(new ErrorDto("USER_NOT_FOUND", "Utilisateur non trouvé")).build();
        OffsetDateTime now = OffsetDateTime.now();
        OtpCodeEntity otp = OtpCodeEntity.find("user = ?1 and usedAt is null and expiresAt > ?2 order by createdAt desc", user, now).firstResult();
        if (otp == null) return Response.status(Response.Status.UNAUTHORIZED)
            .entity(new ErrorDto("INVALID_OTP", "Code OTP invalide ou expiré")).build();
        if (otp.attemptCount >= 5) return Response.status(Response.Status.TOO_MANY_REQUESTS)
            .entity(new ErrorDto("TOO_MANY_ATTEMPTS", "Trop de tentatives, veuillez demander un nouveau code")).build();
        otp.attemptCount += 1;
        if (!otp.code.equals(req.code())) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorDto("INVALID_OTP", "Code OTP incorrect")).build();
        }
        otp.usedAt = now;
        user.phoneVerified = true;
        String access = tokenService.generateAccessToken(user);
        String rawRefresh = UUID.randomUUID().toString();
        String refreshHash = passwordService.hash(rawRefresh);
        tokenService.issueRefreshToken(user, ua, ip, refreshHash);
        return Response.ok(new AuthResponse(access, rawRefresh, AuthMappers.toDto(user))).build();
    }

    @POST
    @Path("/refresh")
    @Transactional
    public Response refresh(RefreshRequest req) {
        String raw = req.refreshToken();
        if (raw == null || raw.isBlank()) return Response.status(Response.Status.BAD_REQUEST).build();
        // Recherche naïve d'un token correspondant (P0)
        List<RefreshTokenEntity> tokens = RefreshTokenEntity.listAll();
        Optional<RefreshTokenEntity> match = tokens.stream()
                .filter(rt -> passwordService.matches(raw, rt.tokenHash))
                .findFirst();
        if (match.isEmpty() || !tokenService.isRefreshValid(match.get())) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        UserEntity user = match.get().user;
        String access = tokenService.generateAccessToken(user);
        return Response.ok(new AuthResponse(access, raw, AuthMappers.toDto(user))).build();
    }

    @POST
    @Path("/logout")
    @Authenticated
    @Transactional
    public Response logout(RefreshRequest req) {
        String raw = req.refreshToken();
        if (raw == null) return Response.status(Response.Status.BAD_REQUEST).build();
        List<RefreshTokenEntity> tokens = RefreshTokenEntity.listAll();
        tokens.stream().filter(rt -> passwordService.matches(raw, rt.tokenHash)).findFirst()
                .ifPresent(tokenService::revokeRefresh);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    private static UserEntity.Role parseRoleOrDefault(String role, UserEntity.Role def) {
        if (role == null) return def;
        try { return UserEntity.Role.valueOf(role.toUpperCase()); } catch (Exception e) { return def; }
    }

    private static String firstIp(String xff) {
        if (xff == null || xff.isBlank()) return null;
        int idx = xff.indexOf(',');
        return (idx >= 0 ? xff.substring(0, idx) : xff).trim();
    }

    // Helper method to get current user
    private UserEntity currentUser() {
        String principalName = identity.getPrincipal().getName();
        
        // Try to find by email first
        UserEntity user = UserEntity.find("email", principalName).firstResult();
        if (user != null) {
            return user;
        }
        
        // If not found by email, try by ID
        try {
            UUID userId = UUID.fromString(principalName);
            user = UserEntity.findById(userId);
            if (user != null) {
                return user;
            }
        } catch (IllegalArgumentException e) {
            // Principal name is not a valid UUID
        }
        
        return null;
    }

    @POST
    @Path("/request-role-change")
    @Authenticated
    @Transactional
    public Response requestRoleChange(RoleChangeRequestDto request) {
        if (request == null || request.requestedRole() == null || request.requestedRole().isBlank()) {
            return badRequest("requestedRole est obligatoire");
        }

        UserEntity user = currentUser();
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorDto("UNAUTHORIZED", "Utilisateur non trouvé")).build();
        }

        // Vérifier si l'utilisateur a déjà une demande en attente
        RoleChangeRequestEntity existingRequest = RoleChangeRequestEntity.find(
            "user = ?1 AND status = 'PENDING'", user
        ).firstResult();

        if (existingRequest != null) {
            return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorDto("PENDING_REQUEST", "Vous avez déjà une demande en attente")).build();
        }

        // Créer la nouvelle demande
        RoleChangeRequestEntity roleRequest = new RoleChangeRequestEntity();
        roleRequest.setUser(user);
        roleRequest.setRequestedRole(request.requestedRole().toUpperCase());
        roleRequest.setReason(request.reason());
        roleRequest.setStatus("PENDING");
        roleRequest.setCreatedAt(OffsetDateTime.now());
        roleRequest.persist();

        return Response.ok(new RoleChangeRequestResponseDto(
            roleRequest.id,
            roleRequest.getRequestedRole(),
            roleRequest.getStatus(),
            roleRequest.getReason(),
            roleRequest.getCreatedAt()
        )).build();
    }

    @POST
    @Path("/forgot-password")
    @Transactional
    public Response forgotPassword(ForgotPasswordRequest req) {
        if (req == null || req.email() == null || req.email().isBlank()) {
            return badRequest("Email est obligatoire");
        }

        String email = req.email().trim().toLowerCase();
        UserEntity user = UserEntity.find("email", email).firstResult();
        
        // Pour des raisons de sécurité, on ne révèle pas si l'email existe ou non
        if (user == null) {
            // Retourner toujours un succès pour éviter l'énumération d'emails
            return Response.status(Response.Status.NO_CONTENT).build();
        }

        // Vérifier le cooldown (60 secondes)
        OffsetDateTime now = OffsetDateTime.now();
        OtpCodeEntity lastReset = OtpCodeEntity.find(
            "user = ?1 and purpose = 'RESET_PASSWORD' order by createdAt desc", 
            user
        ).firstResult();
        
        if (lastReset != null && lastReset.createdAt != null && 
            ChronoUnit.SECONDS.between(lastReset.createdAt, now) < 60) {
            return Response.status(Response.Status.TOO_MANY_REQUESTS)
                .entity(new ErrorDto("RATE_LIMIT", "Veuillez attendre 60 secondes avant de demander un nouveau code")).build();
        }

        // Max 3 demandes par jour
        OffsetDateTime startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        long countToday = OtpCodeEntity.find(
            "user = ?1 and purpose = 'RESET_PASSWORD' and createdAt >= ?2", 
            user, startOfDay
        ).count();
        
        if (countToday >= 3) {
            return Response.status(Response.Status.TOO_MANY_REQUESTS)
                .entity(new ErrorDto("RATE_LIMIT", "Maximum 3 demandes de réinitialisation par jour")).build();
        }

        // Générer un token de réinitialisation
        String resetToken = UUID.randomUUID().toString();
        OtpCodeEntity resetCode = new OtpCodeEntity();
        resetCode.id = UUID.randomUUID();
        resetCode.user = user;
        resetCode.code = resetToken;
        resetCode.channel = OtpCodeEntity.Channel.EMAIL;
        resetCode.purpose = OtpCodeEntity.Purpose.RESET_PASSWORD;
        resetCode.expiresAt = now.plusHours(1); // Token valide 1 heure
        resetCode.attemptCount = 0;
        resetCode.createdAt = now;
        resetCode.persist();

        // TODO: Envoyer l'email avec le lien de réinitialisation
        // emailService.sendPasswordResetEmail(user.email, user.firstName, resetToken);

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @POST
    @Path("/reset-password")
    @Transactional
    public Response resetPassword(ResetPasswordRequest req) {
        if (req == null || req.token() == null || req.newPassword() == null ||
            req.token().isBlank() || req.newPassword().isBlank()) {
            return badRequest("Token et nouveau mot de passe sont obligatoires");
        }

        if (req.newPassword().length() < 8) {
            return badRequest("Le mot de passe doit contenir au moins 8 caractères");
        }

        OffsetDateTime now = OffsetDateTime.now();
        OtpCodeEntity resetCode = OtpCodeEntity.find(
            "code = ?1 and purpose = 'RESET_PASSWORD' and usedAt is null and expiresAt > ?2", 
            req.token(), now
        ).firstResult();

        if (resetCode == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorDto("INVALID_TOKEN", "Token de réinitialisation invalide ou expiré")).build();
        }

        if (resetCode.attemptCount >= 5) {
            return Response.status(Response.Status.TOO_MANY_REQUESTS)
                .entity(new ErrorDto("TOO_MANY_ATTEMPTS", "Trop de tentatives, veuillez demander un nouveau token")).build();
        }

        // Marquer le token comme utilisé
        resetCode.usedAt = now;
        resetCode.attemptCount += 1;

        // Mettre à jour le mot de passe de l'utilisateur
        UserEntity user = resetCode.user;
        user.passwordHash = passwordService.hash(req.newPassword());
        
        // Révoquer tous les refresh tokens de l'utilisateur (forcer reconnexion)
        RefreshTokenEntity.stream("user = ?1 and revokedAt is null", user)
            .map(RefreshTokenEntity.class::cast)
            .forEach(rt -> tokenService.revokeRefresh(rt));

        return Response.status(Response.Status.NO_CONTENT).build();
    }
    
}


