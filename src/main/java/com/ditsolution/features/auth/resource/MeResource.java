package com.ditsolution.features.auth.resource;

import com.ditsolution.features.auth.dto.AuthDtos.UpdateMeRequest;
import com.ditsolution.features.auth.dto.AuthDtos.ChangePasswordRequest;
import com.ditsolution.features.auth.dto.ProfileUpdateDto;
import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.auth.mapper.AuthMappers;
import com.ditsolution.features.auth.service.PasswordService;
import com.ditsolution.features.auth.entity.RefreshTokenEntity;
import com.ditsolution.features.auth.service.TokenService;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;

@RequestScoped
@Path("/me")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class MeResource {

    // cette ressource gère le profil de l’utilisateur connecté via l’URL /me (protégée par JWT)

    @Inject SecurityIdentity identity;
    @Inject PasswordService passwordService;
    @Inject TokenService tokenService;

    // GET /me : retourne le profil du user courant (à partir du sub du JWT) sous forme de UserDto.
    @GET
    public Response me() {
        var user = currentUser();
        return user != null
          ? Response.ok(AuthMappers.toDto(user)).build()
          : unauthorized();
      }

    // PATCH /me/password : changement de mot de passe sécurisé
    @PATCH
    @Path("/password")
    @Transactional
    public Response changePassword(ChangePasswordRequest req){
        var user = currentUser();
        if (user == null) return unauthorized();
        if (req == null || req.currentPassword()==null || req.newPassword()==null
            || req.currentPassword().isBlank() || req.newPassword().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorDto("VALIDATION_ERROR","Champs requis"))
                .build();
        }
        if (!passwordService.matches(req.currentPassword(), user.passwordHash)){
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorDto("INVALID_CREDENTIALS","Mot de passe actuel invalide"))
                .build();
        }
        if (req.newPassword().length() < 8){
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorDto("WEAK_PASSWORD","Le nouveau mot de passe est trop court"))
                .build();
        }
        user.passwordHash = passwordService.hash(req.newPassword());
        // Révoquer tous les refresh tokens de l'utilisateur (forcer reconnexion)
        RefreshTokenEntity.stream("user = ?1 and revokedAt is null", user)
            .map(RefreshTokenEntity.class::cast)
            .forEach(rt -> tokenService.revokeRefresh(rt));
        return Response.noContent().build();
    }

    // DELETE /me : suspend le compte (soft delete)
    @DELETE
    @Transactional
    public Response suspendMe(){
        var user = currentUser();
        if (user == null) return unauthorized();
        user.status = UserEntity.Status.SUSPENDED;
        // Révoquer tous les refresh tokens de l'utilisateur
        RefreshTokenEntity.stream("user = ?1 and revokedAt is null", user)
            .map(RefreshTokenEntity.class::cast)
            .forEach(rt -> tokenService.revokeRefresh(rt));
        return Response.noContent().build();
    }

    // PATCH /me : permet de mettre à jour ses infos de base (ici firstName, lastName, avatarUrl) et renvoie le profil à jour.
    @PATCH
    @Transactional
    public Response update(UpdateMeRequest req) {
        var user = currentUser();
        if (user == null) return unauthorized();
        if (req.firstName()!=null && !req.firstName().isBlank()) user.firstName = req.firstName().trim();
        if (req.lastName()!=null  && !req.lastName().isBlank())  user.lastName  = req.lastName().trim();
        if (req.avatarUrl()!=null && !req.avatarUrl().isBlank()) user.avatarUrl = req.avatarUrl().trim();
        return Response.ok(AuthMappers.toDto(user)).build();
      }

    // PATCH /me/profile : permet de mettre à jour email, phone et mot de passe
    @PATCH
    @Path("/profile")
    @Transactional
    public Response updateProfile(ProfileUpdateDto req) {
        var user = currentUser();
        if (user == null) return unauthorized();
        
        // Vérifier si l'email existe déjà (sauf pour l'utilisateur actuel)
        if (!user.email.equals(req.email())) {
            var existingUser = UserEntity.find("email", req.email()).firstResult();
            if (existingUser != null) {
                return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorDto("EMAIL_EXISTS", "Cette adresse email est déjà utilisée"))
                    .build();
            }
        }
        
        // Vérifier le mot de passe actuel SEULEMENT si on veut changer le mot de passe
        if (req.newPassword() != null && !req.newPassword().isBlank()) {
            if (req.currentPassword() == null || req.currentPassword().isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("CURRENT_PASSWORD_REQUIRED", "Le mot de passe actuel est requis pour changer le mot de passe"))
                    .build();
            }
            
            if (!passwordService.matches(req.currentPassword(), user.passwordHash)) {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorDto("INVALID_CREDENTIALS", "Mot de passe actuel invalide"))
                    .build();
            }
            
            if (req.newPassword().length() < 6) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("WEAK_PASSWORD", "Le nouveau mot de passe doit contenir au moins 6 caractères"))
                    .build();
            }
            
            user.passwordHash = passwordService.hash(req.newPassword());
            
            // Révoquer tous les refresh tokens de l'utilisateur (forcer reconnexion)
            RefreshTokenEntity.stream("user = ?1 and revokedAt is null", user)
                .map(RefreshTokenEntity.class::cast)
                .forEach(rt -> tokenService.revokeRefresh(rt));
        }
        
        // Mettre à jour les informations (email et téléphone)
        user.email = req.email().trim();
        user.phoneE164 = req.phone().trim();
        
        return Response.ok(AuthMappers.toDto(user)).build();
    }

    // PUT /me/role : changement de rôle direct
    @PUT
    @Path("/role")
    @Transactional
    public Response changeRole(RoleChangeRequest req) {
        var user = currentUser();
        if (user == null) return unauthorized();
        
        if (req == null || req.role() == null || req.role().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorDto("INVALID_REQUEST", "Le rôle est obligatoire"))
                .build();
        }
        
        try {
            // Vérifier que le rôle est valide
            UserEntity.Role newRole = UserEntity.Role.valueOf(req.role().toUpperCase());
            
            // Changer le rôle
            user.setRole(newRole);
            
            // Révoquer tous les refresh tokens pour forcer une reconnexion
            RefreshTokenEntity.stream("user = ?1 and revokedAt is null", user)
                .map(RefreshTokenEntity.class::cast)
                .forEach(rt -> tokenService.revokeRefresh(rt));
            
            return Response.ok(AuthMappers.toDto(user)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorDto("INVALID_ROLE", "Rôle invalide: " + req.role()))
                .build();
        }
    }
    
    // DTO pour le changement de rôle
    public record RoleChangeRequest(String role) {}
    
      private UserEntity currentUser() {
        var p = identity==null ? null : identity.getPrincipal();
        if (p == null) return null;
        try { return UserEntity.findById(UUID.fromString(p.getName())); } catch (Exception e) { return null; }
      }
      
    private Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED)
          .entity(new ErrorDto("UNAUTHORIZED","JWT manquant ou invalide")).build();
      }
      record ErrorDto(String error, String message) {}
}


