package com.ditsolution.features.admin.resource;

import com.ditsolution.features.admin.dto.AdminDashboardDto;
import com.ditsolution.features.admin.service.AdminDashboardService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/admin/dashboard")
@RolesAllowed("ADMIN")
public class AdminDashboardResource {

    @Inject
    AdminDashboardService dashboardService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDashboardStats() {
        AdminDashboardDto stats = dashboardService.getDashboardStats();
        return Response.ok(stats).build();
    }
}
