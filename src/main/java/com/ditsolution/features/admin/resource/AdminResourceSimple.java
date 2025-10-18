package com.ditsolution.features.admin.resource;

import com.ditsolution.features.admin.dto.AdminDashboardDto;
import com.ditsolution.features.admin.dto.AdminUserDto;
import com.ditsolution.features.admin.dto.AdminListingDto;
import com.ditsolution.features.admin.service.AdminDashboardService;
import com.ditsolution.features.admin.service.AdminUserService;
import com.ditsolution.features.admin.service.AdminListingService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/admin")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class AdminResourceSimple {

    @Inject
    AdminDashboardService dashboardService;
    
    @Inject
    AdminUserService userService;
    
    @Inject
    AdminListingService listingService;

    @GET
    @Path("/test")
    public Response test() {
        return Response.ok("Admin endpoints working!").build();
    }
    
    @GET
    @Path("/dashboard")
    public Response getDashboard() {
        try {
            // Utiliser l'injection CDI pour obtenir de vraies données
            AdminDashboardDto dashboard = dashboardService.getDashboardData();
            return Response.ok(dashboard).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"code\":\"DASHBOARD_ERROR\",\"message\":\"Erreur: " + e.getMessage() + "\"}")
                .build();
        }
    }
    
    @GET
    @Path("/users-list")
    public Response getUsers() {
        try {
            // Utiliser directement l'EntityManager pour récupérer tous les utilisateurs
            jakarta.persistence.EntityManager em = jakarta.enterprise.inject.spi.CDI.current().select(jakarta.persistence.EntityManager.class).get();
            jakarta.persistence.TypedQuery<com.ditsolution.features.auth.entity.UserEntity> query = em.createQuery("SELECT u FROM UserEntity u ORDER BY u.createdAt DESC", com.ditsolution.features.auth.entity.UserEntity.class);
            List<com.ditsolution.features.auth.entity.UserEntity> users = query.getResultList();
            
            // Convertir en DTOs simples
            List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
            for (com.ditsolution.features.auth.entity.UserEntity user : users) {
                // Compter les annonces de l'utilisateur
                Long listingsCount = em.createQuery("SELECT COUNT(l) FROM ListingEntity l WHERE l.owner.id = :userId", Long.class)
                    .setParameter("userId", user.getId())
                    .getSingleResult();
                
                java.util.Map<String, Object> dto = new java.util.HashMap<>();
                dto.put("id", user.getId().toString());
                dto.put("email", user.getEmail());
                dto.put("firstName", user.getFirstName());
                dto.put("lastName", user.getLastName());
                dto.put("phoneE164", user.getPhoneE164());
                dto.put("role", user.getRole());
                dto.put("status", user.getStatus());
                dto.put("emailVerified", false); // Default value
                dto.put("phoneVerified", false); // Default value
                dto.put("avatarUrl", user.getAvatarUrl());
                dto.put("createdAt", user.getCreatedAt());
                dto.put("updatedAt", user.getUpdatedAt());
                dto.put("listingsCount", listingsCount);
                result.add(dto);
            }
            
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"code\":\"USERS_ERROR\",\"message\":\"Erreur: " + e.getMessage() + "\"}")
                .build();
        }
    }
    
    @GET
    @Path("/listings-list")
    public Response getListings() {
        try {
            // Utiliser directement l'EntityManager pour récupérer toutes les annonces
            jakarta.persistence.EntityManager em = jakarta.enterprise.inject.spi.CDI.current().select(jakarta.persistence.EntityManager.class).get();
            jakarta.persistence.TypedQuery<com.ditsolution.features.listing.entity.ListingEntity> query = em.createQuery("SELECT l FROM ListingEntity l ORDER BY l.createdAt DESC", com.ditsolution.features.listing.entity.ListingEntity.class);
            List<com.ditsolution.features.listing.entity.ListingEntity> listings = query.getResultList();
            
            // Convertir en DTOs simples
            List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
            for (com.ditsolution.features.listing.entity.ListingEntity listing : listings) {
                java.util.Map<String, Object> dto = new java.util.HashMap<>();
                dto.put("id", listing.getId().toString());
                dto.put("title", listing.getTitle());
                dto.put("description", listing.getDescription());
                dto.put("city", listing.getCity());
                dto.put("district", listing.getDistrict());
                dto.put("price", listing.getPrice());
                dto.put("type", listing.getType());
                dto.put("status", listing.getStatus());
                dto.put("isPremium", false); // Pas de champ isPremium dans ListingEntity
                dto.put("createdAt", listing.getCreatedAt());
                dto.put("ownerEmail", listing.getOwner().getEmail());
                dto.put("ownerFirstName", listing.getOwner().getFirstName());
                dto.put("ownerLastName", listing.getOwner().getLastName());
                       // Récupérer les URLs des photos depuis S3
                       List<String> photoUrls = new java.util.ArrayList<>();
                       
                       // D'abord, essayer de récupérer les vraies photos depuis uploaded_images
                       try {
                           jakarta.persistence.TypedQuery<com.ditsolution.features.storage.entity.UploadedImageEntity> imageQuery = em.createQuery(
                               "SELECT u FROM UploadedImageEntity u WHERE u.userId = :userId AND u.isUsed = true ORDER BY u.createdAt DESC", 
                               com.ditsolution.features.storage.entity.UploadedImageEntity.class);
                           imageQuery.setParameter("userId", listing.getOwner().getId());
                           imageQuery.setMaxResults(5); // Limiter à 5 photos
                           
                           List<com.ditsolution.features.storage.entity.UploadedImageEntity> uploadedImages = imageQuery.getResultList();
                           for (com.ditsolution.features.storage.entity.UploadedImageEntity image : uploadedImages) {
                               if (image.getPublicUrl() != null && !image.getPublicUrl().isEmpty()) {
                                   photoUrls.add(image.getPublicUrl());
                               }
                           }
                       } catch (Exception e) {
                           // En cas d'erreur, utiliser les photos de l'annonce
                           if (listing.getPhotos() != null && !listing.getPhotos().isEmpty()) {
                               for (com.ditsolution.features.listing.entity.ListingPhotoEntity photo : listing.getPhotos()) {
                                   if (photo.getUrl() != null && !photo.getUrl().isEmpty()) {
                                       photoUrls.add(photo.getUrl());
                                   }
                               }
                           }
                       }
                       
                       dto.put("photoUrls", photoUrls);
                result.add(dto);
            }
            
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"code\":\"LISTINGS_ERROR\",\"message\":\"Erreur: " + e.getMessage() + "\"}")
                .build();
        }
    }
    
    @GET
    @Path("/listing-details/{id}")
    public Response getListingById(@PathParam("id") String id) {
        try {
            // Utiliser l'injection CDI pour obtenir les détails d'une annonce
            AdminListingDto listing = listingService.getListingById(java.util.UUID.fromString(id));
            return Response.ok(listing).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"code\":\"LISTING_NOT_FOUND\",\"message\":\"Annonce non trouvée: " + e.getMessage() + "\"}")
                .build();
        }
    }
    
           @GET
           @Path("/listing-photos/{id}")
           public Response getListingPhotos(@PathParam("id") String id) {
               try {
                   // Récupérer l'annonce avec ses photos
                   jakarta.persistence.EntityManager em = jakarta.enterprise.inject.spi.CDI.current().select(jakarta.persistence.EntityManager.class).get();
                   com.ditsolution.features.listing.entity.ListingEntity listing = em.find(com.ditsolution.features.listing.entity.ListingEntity.class, java.util.UUID.fromString(id));
                   
                   if (listing == null) {
                       return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"code\":\"LISTING_NOT_FOUND\",\"message\":\"Annonce non trouvée\"}")
                           .build();
                   }
                   
                   // Construire la réponse avec les photos
                   java.util.Map<String, Object> response = new java.util.HashMap<>();
                   response.put("listingId", listing.getId().toString());
                   response.put("title", listing.getTitle());
                   
                   // Récupérer les photos avec leurs URLs S3
                   List<java.util.Map<String, Object>> photos = new java.util.ArrayList<>();
                   if (listing.getPhotos() != null && !listing.getPhotos().isEmpty()) {
                       for (com.ditsolution.features.listing.entity.ListingPhotoEntity photo : listing.getPhotos()) {
                           java.util.Map<String, Object> photoData = new java.util.HashMap<>();
                           photoData.put("id", photo.getId().toString());
                           photoData.put("url", photo.getUrl());
                           photoData.put("ordering", photo.getOrdering());
                           photos.add(photoData);
                       }
                   }
                   response.put("photos", photos);
                   
                   return Response.ok(response).build();
               } catch (Exception e) {
                   return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                       .entity("{\"code\":\"PHOTOS_ERROR\",\"message\":\"Erreur lors de la récupération des photos: " + e.getMessage() + "\"}")
                       .build();
               }
           }
           
           @GET
           @Path("/user-details/{id}")
           public Response getUserDetails(@PathParam("id") String id) {
               try {
                   // Récupérer les détails complets d'un utilisateur
                   jakarta.persistence.EntityManager em = jakarta.enterprise.inject.spi.CDI.current().select(jakarta.persistence.EntityManager.class).get();
                   com.ditsolution.features.auth.entity.UserEntity user = em.find(com.ditsolution.features.auth.entity.UserEntity.class, java.util.UUID.fromString(id));
                   
                   if (user == null) {
                       return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"code\":\"USER_NOT_FOUND\",\"message\":\"Utilisateur non trouvé\"}")
                           .build();
                   }
                   
                   // Compter les annonces de l'utilisateur
                   Long listingsCount = em.createQuery("SELECT COUNT(l) FROM ListingEntity l WHERE l.owner.id = :userId", Long.class)
                       .setParameter("userId", user.getId())
                       .getSingleResult();
                   
                   // Construire la réponse
                   java.util.Map<String, Object> response = new java.util.HashMap<>();
                   response.put("id", user.getId().toString());
                   response.put("email", user.getEmail());
                   response.put("firstName", user.getFirstName());
                   response.put("lastName", user.getLastName());
                   response.put("phoneE164", user.getPhoneE164());
                   response.put("role", user.getRole());
                   response.put("status", user.getStatus());
                   response.put("emailVerified", false); // Default value
                   response.put("phoneVerified", false); // Default value
                   response.put("avatarUrl", user.getAvatarUrl());
                   response.put("createdAt", user.getCreatedAt());
                   response.put("updatedAt", user.getUpdatedAt());
                   response.put("listingsCount", listingsCount);
                   
                   return Response.ok(response).build();
               } catch (Exception e) {
                   return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                       .entity("{\"code\":\"USER_DETAILS_ERROR\",\"message\":\"Erreur lors de la récupération des détails: " + e.getMessage() + "\"}")
                       .build();
               }
           }
           
           @PUT
           @Path("/user-update/{id}")
           @jakarta.transaction.Transactional
           public Response updateUser(@PathParam("id") String id, java.util.Map<String, Object> request) {
               try {
                   // Récupérer l'utilisateur
                   jakarta.persistence.EntityManager em = jakarta.enterprise.inject.spi.CDI.current().select(jakarta.persistence.EntityManager.class).get();
                   com.ditsolution.features.auth.entity.UserEntity user = em.find(com.ditsolution.features.auth.entity.UserEntity.class, java.util.UUID.fromString(id));
                   
                   if (user == null) {
                       return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"code\":\"USER_NOT_FOUND\",\"message\":\"Utilisateur non trouvé\"}")
                           .build();
                   }
                   
                   // Mettre à jour les champs fournis
                   if (request.containsKey("firstName")) {
                       user.setFirstName((String) request.get("firstName"));
                   }
                   if (request.containsKey("lastName")) {
                       user.setLastName((String) request.get("lastName"));
                   }
                   if (request.containsKey("phoneE164")) {
                       user.setPhoneE164((String) request.get("phoneE164"));
                   }
                   if (request.containsKey("role")) {
                       String newRole = (String) request.get("role");
                       try {
                           com.ditsolution.features.auth.entity.UserEntity.Role role = com.ditsolution.features.auth.entity.UserEntity.Role.valueOf(newRole);
                           user.setRole(role);
                       } catch (IllegalArgumentException e) {
                           return Response.status(Response.Status.BAD_REQUEST)
                               .entity("{\"code\":\"INVALID_ROLE\",\"message\":\"Rôle invalide: " + newRole + "\"}")
                               .build();
                       }
                   }
                   if (request.containsKey("status")) {
                       String newStatus = (String) request.get("status");
                       try {
                           com.ditsolution.features.auth.entity.UserEntity.Status status = com.ditsolution.features.auth.entity.UserEntity.Status.valueOf(newStatus);
                           user.setStatus(status);
                       } catch (IllegalArgumentException e) {
                           return Response.status(Response.Status.BAD_REQUEST)
                               .entity("{\"code\":\"INVALID_STATUS\",\"message\":\"Statut invalide: " + newStatus + "\"}")
                               .build();
                       }
                   }
                   
                   // Sauvegarder les modifications
                   em.merge(user);
                   
                   return Response.ok("{\"message\":\"Utilisateur mis à jour avec succès\"}").build();
               } catch (Exception e) {
                   return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                       .entity("{\"code\":\"UPDATE_ERROR\",\"message\":\"Erreur lors de la mise à jour: " + e.getMessage() + "\"}")
                       .build();
               }
           }
           
           @PATCH
           @Path("/listing-status/{id}")
           @jakarta.transaction.Transactional
           public Response updateListingStatus(@PathParam("id") String id, java.util.Map<String, String> request) {
        try {
            String newStatus = request.get("status");
            if (newStatus == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"code\":\"MISSING_STATUS\",\"message\":\"Statut manquant\"}")
                    .build();
            }
            
            // Utiliser directement l'EntityManager pour mettre à jour le statut
            jakarta.persistence.EntityManager em = jakarta.enterprise.inject.spi.CDI.current().select(jakarta.persistence.EntityManager.class).get();
            com.ditsolution.features.listing.entity.ListingEntity listing = em.find(com.ditsolution.features.listing.entity.ListingEntity.class, java.util.UUID.fromString(id));
            
            if (listing == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"code\":\"LISTING_NOT_FOUND\",\"message\":\"Annonce non trouvée\"}")
                    .build();
            }
            
            // Valider le statut
            try {
                com.ditsolution.features.listing.enums.ListingStatus status = com.ditsolution.features.listing.enums.ListingStatus.valueOf(newStatus);
                listing.setStatus(status);
                em.merge(listing);
                
                return Response.ok("{\"message\":\"Statut mis à jour avec succès\",\"newStatus\":\"" + newStatus + "\"}").build();
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"code\":\"INVALID_STATUS\",\"message\":\"Statut invalide: " + newStatus + "\"}")
                    .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"code\":\"UPDATE_ERROR\",\"message\":\"Erreur lors de la mise à jour: " + e.getMessage() + "\"}")
                .build();
        }
    }
}
