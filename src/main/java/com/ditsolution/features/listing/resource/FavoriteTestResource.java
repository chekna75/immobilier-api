package com.ditsolution.features.listing.resource;

import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.listing.entity.FavoriteEntity;
import com.ditsolution.features.listing.entity.ListingEntity;
import com.ditsolution.features.listing.repository.ListingRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/api/favorites-test")
@Produces(MediaType.APPLICATION_JSON)
public class FavoriteTestResource {

    @Inject
    ListingRepository listingRepository;

    @GET
    @Path("/check-table")
    public Response checkTable() {
        try {
            // Essayer de compter les favoris pour vérifier si la table existe
            long count = FavoriteEntity.count();
            return Response.ok().entity("{\"tableExists\": true, \"count\": " + count + "}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok().entity("{\"tableExists\": false, \"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    @GET
    @Path("/check-user/{userId}")
    public Response checkUser(@PathParam("userId") String userId) {
        try {
            UserEntity user = UserEntity.findById(UUID.fromString(userId));
            if (user != null) {
                return Response.ok().entity("{\"userExists\": true, \"email\": \"" + user.email + "\"}").build();
            } else {
                return Response.ok().entity("{\"userExists\": false}").build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok().entity("{\"userExists\": false, \"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    @GET
    @Path("/check-listing/{listingId}")
    public Response checkListing(@PathParam("listingId") String listingId) {
        try {
            ListingEntity listing = listingRepository.findById(UUID.fromString(listingId));
            if (listing != null) {
                return Response.ok().entity("{\"listingExists\": true, \"title\": \"" + listing.getTitle() + "\"}").build();
            } else {
                return Response.ok().entity("{\"listingExists\": false}").build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok().entity("{\"listingExists\": false, \"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    @POST
    @Path("/test-add-favorite/{userId}/{listingId}")
    @Transactional
    public Response testAddFavorite(@PathParam("userId") String userId, @PathParam("listingId") String listingId) {
        try {
            // Test simple d'ajout de favori
            FavoriteEntity favorite = new FavoriteEntity();
            favorite.setUser(UserEntity.findById(UUID.fromString(userId)));
            favorite.setListing(listingRepository.findById(UUID.fromString(listingId)));
            favorite.persist();
            
            return Response.ok().entity("{\"success\": true, \"favoriteId\": " + favorite.id + "}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok().entity("{\"success\": false, \"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    @POST
    @Path("/create-sequence")
    @Transactional
    public Response createSequence() {
        try {
            // Créer la séquence manquante en utilisant Panache
            FavoriteEntity.getEntityManager().createNativeQuery("CREATE SEQUENCE IF NOT EXISTS favorites_seq START 1").executeUpdate();
            
            return Response.ok().entity("{\"success\": true, \"message\": \"Séquence favorites_seq créée\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok().entity("{\"success\": false, \"error\": \"" + e.getMessage() + "\"}").build();
        }
    }
}