package com.ditsolution.common.utils;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.Map;

public final class HttpErrors {
    private HttpErrors() {}

    public static WebApplicationException badRequest(String code, String msg) {
        var body = Map.of("error", code, "message", msg);
        return new WebApplicationException(
            Response.status(Response.Status.BAD_REQUEST).entity(body).build()
        );
    }

    public static WebApplicationException forbidden(String code, String msg) {
        var body = Map.of("error", code, "message", msg);
        return new WebApplicationException(
            Response.status(Response.Status.FORBIDDEN).entity(body).build()
        );
    }

    public static WebApplicationException notFound(String code, String msg) {
        var body = Map.of("error", code, "message", msg);
        return new WebApplicationException(
            Response.status(Response.Status.NOT_FOUND).entity(body).build()
        );
    }
}