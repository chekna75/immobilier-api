package com.ditsolution.features.admin.resource;

import com.ditsolution.features.admin.dto.AdminLogDto;
import com.ditsolution.features.admin.service.AdminLogService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/admin/logs")
@RolesAllowed("ADMIN")
@Produces(MediaType.APPLICATION_JSON)
public class AdminLogResource {

    @Inject
    AdminLogService logService;

    @GET
    public Response getLogs(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("action") String action,
            @QueryParam("targetType") String targetType,
            @QueryParam("adminId") UUID adminId) {
        
        List<AdminLogDto> logs = logService.getLogs(page, size, action, targetType, adminId);
        long total = logService.getLogsCount(action, targetType, adminId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("logs", logs);
        response.put("total", total);
        response.put("page", page);
        response.put("size", size);
        response.put("totalPages", (total + size - 1) / size);
        
        return Response.ok(response).build();
    }

    @GET
    @Path("/actions")
    public Response getAvailableActions() {
        List<String> actions = logService.getAvailableActions();
        return Response.ok(actions).build();
    }

    @GET
    @Path("/target-types")
    public Response getAvailableTargetTypes() {
        List<String> targetTypes = logService.getAvailableTargetTypes();
        return Response.ok(targetTypes).build();
    }
}
