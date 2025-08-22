package com.ditsolution.features.auth.resource;

import com.ditsolution.features.auth.dto.AuthDtos.UpdateMeRequest;
import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.auth.mapper.AuthMappers;
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

    // GET /me : retourne le profil du user courant (à partir du sub du JWT) sous forme de UserDto.
    @GET
    public Response me() {
        var user = currentUser();
        return user != null
          ? Response.ok(AuthMappers.toDto(user)).build()
          : unauthorized();
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


